package com.doctor.appointment.modules.appointment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.doctor.appointment.common.exception.BizException;
import com.doctor.appointment.modules.appointment.dto.AppointmentAssignDTO;
import com.doctor.appointment.modules.appointment.dto.AppointmentCreateDTO;
import com.doctor.appointment.modules.appointment.entity.Appointment;
import com.doctor.appointment.modules.appointment.mapper.AppointmentMapper;
import com.doctor.appointment.modules.department.entity.Department;
import com.doctor.appointment.modules.department.mapper.DepartmentMapper;
import com.doctor.appointment.modules.schedule.entity.Schedule;
import com.doctor.appointment.modules.schedule.mapper.ScheduleMapper;
import com.doctor.appointment.modules.user.entity.SysUser;
import com.doctor.appointment.modules.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 预约服务：线上预约、线下登记、派单、取消、完成
 */
@Service
@RequiredArgsConstructor
public class AppointmentService extends ServiceImpl<AppointmentMapper, Appointment> {

    private final AppointmentMapper appointmentMapper;
    private final ScheduleMapper scheduleMapper;
    private final SysUserMapper sysUserMapper;
    private final DepartmentMapper departmentMapper;
    private final com.doctor.appointment.modules.user.mapper.DoctorInfoMapper doctorInfoMapper;

    /**
     * 创建预约。
     * 线上预约：患者指定排班号源 → 直接挂到医生，状态为已派单；
     * 未选号源（仅选科室）→ 待派单，由管理员/医生分诊；
     * 线下登记：工作人员代录，逻辑相同。
     */
    @Transactional(rollbackFor = Exception.class)
    public Appointment create(AppointmentCreateDTO dto, Long loginUserId, boolean online) {
        Appointment appointment = new Appointment();
        appointment.setAppointmentNo(generateNo());
        appointment.setPatientId(online ? loginUserId : dto.getPatientId());
        if (appointment.getPatientId() == null) {
            throw new BizException(400, "请指定患者");
        }
        appointment.setDepartmentId(dto.getDepartmentId());
        appointment.setSymptoms(dto.getSymptoms());
        appointment.setSource(online ? "ONLINE" : "OFFLINE");
        appointment.setAppointDate(dto.getAppointDate());
        appointment.setTimeSlot(dto.getTimeSlot());

        if (dto.getScheduleId() != null) {
            // 走号源：校验排班并占用号源（原子 +1，号满失败）
            Schedule schedule = scheduleMapper.selectById(dto.getScheduleId());
            if (schedule == null || schedule.getStatus() != 1) {
                throw new BizException(400, "该排班不可预约");
            }
            if (schedule.getWorkDate().isBefore(LocalDate.now())) {
                throw new BizException(400, "不能预约过往日期的号源");
            }
            // 资质闭环：医生未过审则该号源不可约（与患者端医生列表/号源查询一致）
            com.doctor.appointment.modules.user.entity.DoctorInfo scheduleDoctor = doctorInfoMapper.selectOne(
                    new LambdaQueryWrapper<com.doctor.appointment.modules.user.entity.DoctorInfo>()
                            .eq(com.doctor.appointment.modules.user.entity.DoctorInfo::getUserId, schedule.getDoctorId()));
            if (scheduleDoctor == null || scheduleDoctor.getAuditStatus() == null || scheduleDoctor.getAuditStatus() != 2) {
                throw new BizException(400, "该医生资质未通过审核，暂不可预约");
            }
            if (scheduleMapper.incrBooked(schedule.getId()) == 0) {
                throw new BizException(400, "该时段号源已满");
            }
            appointment.setScheduleId(schedule.getId());
            appointment.setDoctorId(schedule.getDoctorId());
            appointment.setAppointDate(schedule.getWorkDate());
            appointment.setTimeSlot(schedule.getTimeSlot());
            appointment.setStatus(Appointment.STATUS_ASSIGNED);
        } else {
            // 不指定号源：进入待派单池
            if (dto.getAppointDate().isBefore(LocalDate.now())) {
                throw new BizException(400, "就诊日期不能早于今天");
            }
            appointment.setDoctorId(dto.getDoctorId());
            appointment.setStatus(Appointment.STATUS_PENDING);
        }
        save(appointment);
        return appointment;
    }

    /**
     * 派单：将待派单预约分配给医生与时段（仅可派给资质已通过的医生）
     */
    @Transactional(rollbackFor = Exception.class)
    public void assign(AppointmentAssignDTO dto) {
        Appointment appointment = getById(dto.getAppointmentId());
        if (appointment == null) {
            throw new BizException(400, "预约不存在");
        }
        if (appointment.getStatus() != Appointment.STATUS_PENDING) {
            throw new BizException(400, "仅待派单的预约可以派单");
        }
        SysUser doctor = sysUserMapper.selectById(dto.getDoctorId());
        if (doctor == null || !"DOCTOR".equals(doctor.getRole())) {
            throw new BizException(400, "接诊医生不存在");
        }
        com.doctor.appointment.modules.user.entity.DoctorInfo doctorInfo = doctorInfoMapper.selectOne(
                new LambdaQueryWrapper<com.doctor.appointment.modules.user.entity.DoctorInfo>()
                        .eq(com.doctor.appointment.modules.user.entity.DoctorInfo::getUserId, dto.getDoctorId()));
        if (doctorInfo == null || doctorInfo.getAuditStatus() == null || doctorInfo.getAuditStatus() != 2) {
            throw new BizException(400, "该医生资质未通过审核，不能派单");
        }
        if (dto.getAppointDate() == null || dto.getAppointDate().isBefore(LocalDate.now())) {
            throw new BizException(400, "就诊日期不能早于今天");
        }
        appointment.setDoctorId(dto.getDoctorId());
        appointment.setAppointDate(dto.getAppointDate());
        appointment.setTimeSlot(dto.getTimeSlot());
        appointment.setStatus(Appointment.STATUS_ASSIGNED);
        updateById(appointment);
    }

    /**
     * 取消预约：患者取消自己的，医生取消自己接诊的，管理员取消任意
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id, String reason, Long loginUserId, String role) {
        Appointment appointment = getById(id);
        if (appointment == null) {
            throw new BizException(400, "预约不存在");
        }
        if ("PATIENT".equals(role) && !appointment.getPatientId().equals(loginUserId)) {
            throw new BizException(403, "只能取消自己的预约");
        }
        if ("DOCTOR".equals(role) && !loginUserId.equals(appointment.getDoctorId())) {
            throw new BizException(403, "只能取消自己接诊的预约");
        }
        if (appointment.getStatus() == Appointment.STATUS_COMPLETED
                || appointment.getStatus() == Appointment.STATUS_CANCELLED) {
            throw new BizException(400, "当前状态不可取消");
        }
        appointment.setStatus(Appointment.STATUS_CANCELLED);
        appointment.setCancelReason(reason);
        updateById(appointment);
        // 释放号源
        if (appointment.getScheduleId() != null) {
            scheduleMapper.decrBooked(appointment.getScheduleId());
        }
    }

    /** 标记就诊完成（医生操作） */
    public void complete(Long id, Long doctorUserId) {
        Appointment appointment = getById(id);
        if (appointment == null) {
            throw new BizException(400, "预约不存在");
        }
        if (!appointment.getDoctorId().equals(doctorUserId)) {
            throw new BizException(403, "只能操作自己的接诊单");
        }
        if (appointment.getStatus() != Appointment.STATUS_ASSIGNED) {
            throw new BizException(400, "仅已派单的预约可以标记完成");
        }
        appointment.setStatus(Appointment.STATUS_COMPLETED);
        updateById(appointment);
    }

    /**
     * 预约详情（按角色隔离：患者仅自己的，医生仅自己接诊的，管理员全部）
     */
    public Appointment detailFor(Long id, String role, Long userId) {
        Appointment appointment = getById(id);
        if (appointment == null) {
            return null;
        }
        if ("PATIENT".equals(role) && !appointment.getPatientId().equals(userId)) {
            throw new BizException(403, "无权查看该预约");
        }
        if ("DOCTOR".equals(role) && !userId.equals(appointment.getDoctorId())) {
            throw new BizException(403, "无权查看该预约");
        }
        fillNames(List.of(appointment));
        return appointment;
    }

    /**
     * 分页查询：患者看自己的，医生看自己的接诊单，管理员看全部
     */
    public Page<Appointment> pageAppointments(int pageNum, int pageSize, Integer status,
                                              String role, Long userId, Long departmentId) {
        LambdaQueryWrapper<Appointment> wrapper = new LambdaQueryWrapper<Appointment>()
                .eq(status != null, Appointment::getStatus, status)
                .eq(departmentId != null, Appointment::getDepartmentId, departmentId)
                .orderByDesc(Appointment::getCreatedAt);
        if ("PATIENT".equals(role)) {
            wrapper.eq(Appointment::getPatientId, userId);
        } else if ("DOCTOR".equals(role)) {
            wrapper.eq(Appointment::getDoctorId, userId);
        }
        Page<Appointment> page = page(new Page<>(pageNum, pageSize), wrapper);
        fillNames(page.getRecords());
        return page;
    }

    private void fillNames(List<Appointment> list) {
        if (list.isEmpty()) {
            return;
        }
        List<Long> userIds = list.stream()
                .flatMap(a -> java.util.stream.Stream.of(a.getPatientId(), a.getDoctorId()))
                .filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, String> userMap = userIds.isEmpty() ? Map.of() :
                sysUserMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(SysUser::getId, SysUser::getRealName));
        List<Long> deptIds = list.stream().map(Appointment::getDepartmentId)
                .filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, String> deptMap = new HashMap<>();
        if (!deptIds.isEmpty()) {
            departmentMapper.selectBatchIds(deptIds).forEach(d -> deptMap.put(d.getId(), d.getName()));
        }
        for (Appointment a : list) {
            a.setPatientName(userMap.get(a.getPatientId()));
            a.setDoctorName(userMap.get(a.getDoctorId()));
            a.setDepartmentName(deptMap.get(a.getDepartmentId()));
        }
    }

    private String generateNo() {
        return "AP" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + System.currentTimeMillis() % 100_000
                + ThreadLocalRandom.current().nextInt(100, 999);
    }
}
