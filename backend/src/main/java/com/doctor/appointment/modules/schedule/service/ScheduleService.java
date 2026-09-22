package com.doctor.appointment.modules.schedule.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.doctor.appointment.common.exception.BizException;
import com.doctor.appointment.modules.appointment.entity.Appointment;
import com.doctor.appointment.modules.appointment.mapper.AppointmentMapper;
import com.doctor.appointment.modules.department.entity.Department;
import com.doctor.appointment.modules.department.mapper.DepartmentMapper;
import com.doctor.appointment.modules.schedule.dto.ScheduleBatchDTO;
import com.doctor.appointment.modules.schedule.entity.Schedule;
import com.doctor.appointment.modules.schedule.mapper.ScheduleMapper;
import com.doctor.appointment.modules.user.entity.DoctorInfo;
import com.doctor.appointment.modules.user.entity.SysUser;
import com.doctor.appointment.modules.user.mapper.DoctorInfoMapper;
import com.doctor.appointment.modules.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 排班服务：批量排班、冲突检查、停诊/调班动态管控
 */
@Service
@RequiredArgsConstructor
public class ScheduleService extends ServiceImpl<ScheduleMapper, Schedule> {

    private final ScheduleMapper scheduleMapper;
    private final SysUserMapper sysUserMapper;
    private final DoctorInfoMapper doctorInfoMapper;
    private final DepartmentMapper departmentMapper;
    private final AppointmentMapper appointmentMapper;

    /**
     * 批量排班：日期范围 × 时段，自动跳过重复排班，返回成功条数。
     * doctorId 由控制器按角色解析后传入：医生角色强制为本人，管理员指定目标医生。
     */
    @Transactional(rollbackFor = Exception.class)
    public int batchCreate(ScheduleBatchDTO dto, Long doctorId, boolean isAdmin) {
        if (doctorId == null) {
            throw new BizException(400, isAdmin ? "请选择医生" : "请指定医生");
        }
        // 资质闭环：未通过执业资质审核的医生不能排班出诊
        requireApproved(doctorId);
        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new BizException(400, "结束日期不能早于开始日期");
        }
        if (dto.getStartDate().isBefore(LocalDate.now())) {
            throw new BizException(400, "开始日期不能早于今天");
        }
        if (dto.getEndDate().isAfter(dto.getStartDate().plusDays(31))) {
            throw new BizException(400, "单次批量排班不能超过31天");
        }
        int created = 0;
        for (LocalDate date = dto.getStartDate(); !date.isAfter(dto.getEndDate()); date = date.plusDays(1)) {
            for (String slot : dto.getTimeSlots()) {
                long exists = count(new LambdaQueryWrapper<Schedule>()
                        .eq(Schedule::getDoctorId, doctorId)
                        .eq(Schedule::getWorkDate, date)
                        .eq(Schedule::getTimeSlot, slot));
                if (exists > 0) {
                    continue; // 已存在则跳过，避免重复排班冲突
                }
                Schedule schedule = new Schedule();
                schedule.setDoctorId(doctorId);
                schedule.setWorkDate(date);
                schedule.setTimeSlot(slot);
                schedule.setMaxCount(dto.getMaxCount());
                schedule.setBookedCount(0);
                schedule.setStatus(1);
                try {
                    save(schedule);
                    created++;
                } catch (org.springframework.dao.DuplicateKeyException e) {
                    // 并发提交撞唯一键时按"重复已跳过"处理，不中断整批
                    continue;
                }
            }
        }
        return created;
    }

    /** 分页查询（含医生姓名、科室名） */
    public Page<Schedule> pageSchedules(int pageNum, int pageSize, Long doctorId,
                                        LocalDate startDate, LocalDate endDate, Integer status) {
        LambdaQueryWrapper<Schedule> wrapper = new LambdaQueryWrapper<Schedule>()
                .eq(doctorId != null, Schedule::getDoctorId, doctorId)
                .ge(startDate != null, Schedule::getWorkDate, startDate)
                .le(endDate != null, Schedule::getWorkDate, endDate)
                .eq(status != null, Schedule::getStatus, status)
                .orderByAsc(Schedule::getWorkDate);
        Page<Schedule> page = page(new Page<>(pageNum, pageSize), wrapper);
        fillExtra(page.getRecords());
        return page;
    }

    /** 查询某医生某日期可预约的排班（患者端；医生未过资质审核时不开放号源） */
    public List<Schedule> listAvailable(Long doctorId, LocalDate date) {
        DoctorInfo info = doctorInfoMapper.selectOne(
                new LambdaQueryWrapper<DoctorInfo>().eq(DoctorInfo::getUserId, doctorId));
        if (info == null || info.getAuditStatus() == null || info.getAuditStatus() != 2) {
            return List.of();
        }
        List<Schedule> list = list(new LambdaQueryWrapper<Schedule>()
                .eq(Schedule::getDoctorId, doctorId)
                .eq(Schedule::getWorkDate, date)
                .eq(Schedule::getStatus, 1)
                .apply("booked_count < max_count"));
        fillExtra(list);
        return list;
    }

    /** 校验医生执业资质已通过审核，未通过则抛业务异常 */
    private void requireApproved(Long doctorUserId) {
        DoctorInfo info = doctorInfoMapper.selectOne(
                new LambdaQueryWrapper<DoctorInfo>().eq(DoctorInfo::getUserId, doctorUserId));
        if (info == null || info.getAuditStatus() == null || info.getAuditStatus() != 2) {
            throw new BizException(403, "执业资质未通过审核，暂不能排班出诊；请先在「资质审核」页提交材料");
        }
    }

    /**
     * 停诊/调班/恢复：停诊时联动取消未完成的预约，返回联动取消的预约笔数。
     * 医生仅能操作自己的排班，管理员可操作全部。
     */
    @Transactional(rollbackFor = Exception.class)
    public int changeStatus(Long id, Integer status, String remark, Long operatorId, boolean isAdmin) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException(400, "状态参数不合法");
        }
        Schedule schedule = getById(id);
        if (schedule == null) {
            throw new BizException(400, "排班不存在");
        }
        if (!isAdmin && !schedule.getDoctorId().equals(operatorId)) {
            throw new BizException(403, "只能操作自己的排班");
        }
        schedule.setStatus(status);
        schedule.setRemark(remark);
        updateById(schedule);
        int cancelled = 0;
        if (status == 0) {
            // 停诊：取消该排班下所有待派单/已派单预约
            List<Appointment> affected = appointmentMapper.selectList(new LambdaQueryWrapper<Appointment>()
                    .eq(Appointment::getScheduleId, id)
                    .in(Appointment::getStatus, Appointment.STATUS_PENDING, Appointment.STATUS_ASSIGNED));
            for (Appointment a : affected) {
                a.setStatus(Appointment.STATUS_CANCELLED);
                a.setCancelReason("医生停诊：" + (remark == null ? "" : remark));
                appointmentMapper.updateById(a);
                scheduleMapper.decrBooked(id);
                cancelled++;
            }
        }
        return cancelled;
    }

    /** 删除排班（仅限无人预约的排班；医生仅能删除自己的） */
    public void deleteSchedule(Long id, Long operatorId, boolean isAdmin) {
        Schedule schedule = getById(id);
        if (schedule == null) {
            return;
        }
        if (!isAdmin && !schedule.getDoctorId().equals(operatorId)) {
            throw new BizException(403, "只能操作自己的排班");
        }
        if (schedule.getBookedCount() != null && schedule.getBookedCount() > 0) {
            throw new BizException(400, "该排班已有预约，请先停诊并处理关联预约");
        }
        removeById(id);
    }

    private void fillExtra(List<Schedule> list) {
        if (list.isEmpty()) {
            return;
        }
        List<Long> doctorIds = list.stream().map(Schedule::getDoctorId).distinct().collect(Collectors.toList());
        Map<Long, String> nameMap = sysUserMapper.selectBatchIds(doctorIds).stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getRealName));
        // 医生 -> 科室（departmentId 可能为空，不能放 Collectors.toMap，否则 NPE）
        Map<Long, Long> doctorDeptMap = new HashMap<>();
        doctorInfoMapper.selectList(
                        new LambdaQueryWrapper<DoctorInfo>().in(DoctorInfo::getUserId, doctorIds))
                .forEach(info -> doctorDeptMap.put(info.getUserId(), info.getDepartmentId()));
        List<Long> deptIds = doctorDeptMap.values().stream().filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, String> deptMap = deptIds.isEmpty() ? Map.of() :
                departmentMapper.selectBatchIds(deptIds).stream()
                        .collect(Collectors.toMap(Department::getId, Department::getName));
        for (Schedule s : list) {
            s.setDoctorName(nameMap.get(s.getDoctorId()));
            Long deptId = doctorDeptMap.get(s.getDoctorId());
            s.setDepartmentName(deptId == null ? null : deptMap.get(deptId));
        }
    }
}
