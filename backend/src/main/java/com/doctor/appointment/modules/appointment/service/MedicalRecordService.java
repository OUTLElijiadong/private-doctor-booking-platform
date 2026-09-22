package com.doctor.appointment.modules.appointment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.doctor.appointment.common.exception.BizException;
import com.doctor.appointment.modules.appointment.dto.MedicalRecordCreateDTO;
import com.doctor.appointment.modules.appointment.entity.Appointment;
import com.doctor.appointment.modules.appointment.entity.MedicalRecord;
import com.doctor.appointment.modules.appointment.mapper.AppointmentMapper;
import com.doctor.appointment.modules.appointment.mapper.MedicalRecordMapper;
import com.doctor.appointment.modules.user.entity.SysUser;
import com.doctor.appointment.modules.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 病历服务：就诊结束后归档患者病情，形成个人电子病历档案
 */
@Service
@RequiredArgsConstructor
public class MedicalRecordService extends ServiceImpl<MedicalRecordMapper, MedicalRecord> {

    private final AppointmentMapper appointmentMapper;
    private final SysUserMapper sysUserMapper;

    /**
     * 医生填写病历：校验接诊关系，自动将预约标记为已完成
     */
    @Transactional(rollbackFor = Exception.class)
    public MedicalRecord create(Long doctorUserId, MedicalRecordCreateDTO dto) {
        Appointment appointment = appointmentMapper.selectById(dto.getAppointmentId());
        if (appointment == null) {
            throw new BizException(400, "预约记录不存在");
        }
        if (!doctorUserId.equals(appointment.getDoctorId())) {
            throw new BizException(403, "只能为自己的接诊单填写病历");
        }
        if (appointment.getStatus() != Appointment.STATUS_ASSIGNED) {
            throw new BizException(400, "仅已派单的预约可以填写病历");
        }
        long exists = count(new LambdaQueryWrapper<MedicalRecord>()
                .eq(MedicalRecord::getAppointmentId, dto.getAppointmentId()));
        if (exists > 0) {
            throw new BizException(400, "该预约已填写病历，请勿重复提交");
        }
        MedicalRecord record = new MedicalRecord();
        record.setRecordNo("MR" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + System.currentTimeMillis() % 100_000
                + ThreadLocalRandom.current().nextInt(100, 999));
        record.setAppointmentId(appointment.getId());
        record.setPatientId(appointment.getPatientId());
        record.setDoctorId(doctorUserId);
        record.setChiefComplaint(dto.getChiefComplaint());
        record.setDiagnosis(dto.getDiagnosis());
        record.setTreatment(dto.getTreatment());
        record.setVisitTime(LocalDateTime.now());
        save(record);
        // 就诊管控：预约流转为已完成
        appointment.setStatus(Appointment.STATUS_COMPLETED);
        appointmentMapper.updateById(appointment);
        return record;
    }

    /** 病历详情（按角色隔离：患者仅自己的，医生仅自己接诊的，管理员全部） */
    public MedicalRecord detailFor(Long id, String role, Long userId) {
        MedicalRecord record = getById(id);
        if (record == null) {
            return null;
        }
        if ("PATIENT".equals(role) && !record.getPatientId().equals(userId)) {
            throw new BizException(403, "无权查看该病历");
        }
        if ("DOCTOR".equals(role) && !record.getDoctorId().equals(userId)) {
            throw new BizException(403, "无权查看该病历");
        }
        fillNames(List.of(record));
        return record;
    }

    /** 分页查询：患者查自己的病历，医生查自己接诊的，管理员查全部 */
    public Page<MedicalRecord> pageRecords(int pageNum, int pageSize, String role, Long userId, Long patientId) {
        LambdaQueryWrapper<MedicalRecord> wrapper = new LambdaQueryWrapper<MedicalRecord>()
                .orderByDesc(MedicalRecord::getCreatedAt);
        if ("PATIENT".equals(role)) {
            wrapper.eq(MedicalRecord::getPatientId, userId);
        } else if ("DOCTOR".equals(role)) {
            wrapper.eq(MedicalRecord::getDoctorId, userId)
                    .eq(patientId != null, MedicalRecord::getPatientId, patientId);
        } else {
            wrapper.eq(patientId != null, MedicalRecord::getPatientId, patientId);
        }
        Page<MedicalRecord> page = page(new Page<>(pageNum, pageSize), wrapper);
        fillNames(page.getRecords());
        return page;
    }

    private void fillNames(List<MedicalRecord> list) {
        if (list.isEmpty()) {
            return;
        }
        List<Long> userIds = list.stream()
                .flatMap(r -> java.util.stream.Stream.of(r.getPatientId(), r.getDoctorId()))
                .distinct().collect(Collectors.toList());
        Map<Long, String> nameMap = sysUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getRealName));
        for (MedicalRecord r : list) {
            r.setPatientName(nameMap.get(r.getPatientId()));
            r.setDoctorName(nameMap.get(r.getDoctorId()));
        }
    }
}
