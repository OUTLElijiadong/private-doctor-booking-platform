package com.doctor.appointment.modules.stats.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.doctor.appointment.modules.appointment.entity.Appointment;
import com.doctor.appointment.modules.appointment.entity.MedicalRecord;
import com.doctor.appointment.modules.appointment.mapper.AppointmentMapper;
import com.doctor.appointment.modules.appointment.mapper.MedicalRecordMapper;
import com.doctor.appointment.modules.department.entity.Department;
import com.doctor.appointment.modules.department.mapper.DepartmentMapper;
import com.doctor.appointment.modules.medication.entity.Medication;
import com.doctor.appointment.modules.medication.mapper.MedicationMapper;
import com.doctor.appointment.modules.prescription.entity.Prescription;
import com.doctor.appointment.modules.prescription.mapper.PrescriptionItemMapper;
import com.doctor.appointment.modules.prescription.mapper.PrescriptionMapper;
import com.doctor.appointment.modules.user.entity.DoctorInfo;
import com.doctor.appointment.modules.user.entity.Evaluation;
import com.doctor.appointment.modules.user.entity.SysUser;
import com.doctor.appointment.modules.user.mapper.DoctorInfoMapper;
import com.doctor.appointment.modules.user.mapper.EvaluationMapper;
import com.doctor.appointment.modules.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统计分析服务（可视化分析与系统分析数据源）
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private final SysUserMapper sysUserMapper;
    private final DoctorInfoMapper doctorInfoMapper;
    private final DepartmentMapper departmentMapper;
    private final AppointmentMapper appointmentMapper;
    private final PrescriptionMapper prescriptionMapper;
    private final PrescriptionItemMapper prescriptionItemMapper;
    private final MedicationMapper medicationMapper;
    private final EvaluationMapper evaluationMapper;
    private final MedicalRecordMapper medicalRecordMapper;

    /** 登录页公开平台数据（仅聚合数字，不含任何个人信息） */
    public Map<String, Object> publicStats() {
        Map<String, Object> map = new HashMap<>();
        map.put("doctorCount", doctorInfoMapper.selectCount(
                new LambdaQueryWrapper<DoctorInfo>().eq(DoctorInfo::getAuditStatus, 2)));
        map.put("departmentCount", departmentMapper.selectCount(
                new LambdaQueryWrapper<Department>().eq(Department::getStatus, 1)));
        map.put("appointmentTotal", appointmentMapper.selectCount(null));
        return map;
    }

    /** 总览卡片数据 */
    public Map<String, Object> overview() {
        Map<String, Object> map = new HashMap<>();
        map.put("patientCount", sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getRole, "PATIENT")));
        map.put("doctorCount", doctorInfoMapper.selectCount(
                new LambdaQueryWrapper<DoctorInfo>().eq(DoctorInfo::getAuditStatus, 2)));
        map.put("appointmentTotal", appointmentMapper.selectCount(null));
        // 今日预约量
        map.put("appointmentToday", appointmentMapper.selectCount(new LambdaQueryWrapper<Appointment>()
                .between(Appointment::getCreatedAt, LocalDateTime.of(LocalDate.now(), LocalTime.MIN),
                        LocalDateTime.of(LocalDate.now(), LocalTime.MAX))));
        // 待派单数量
        map.put("pendingAppointment", appointmentMapper.selectCount(new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getStatus, Appointment.STATUS_PENDING)));
        // 待缴费处方
        map.put("unpaidPrescription", prescriptionMapper.selectCount(new LambdaQueryWrapper<Prescription>()
                .eq(Prescription::getStatus, Prescription.STATUS_UNPAID)));
        // 库存预警药品数（库存低于预警线）
        map.put("lowStockMedication", medicationMapper.selectCount(
                new LambdaQueryWrapper<Medication>().apply("stock <= warning_stock")));
        return map;
    }

    /** 近7天预约趋势（按天补零，保证 x 轴连续） */
    public List<Map<String, Object>> appointmentTrend() {
        return fillZero(appointmentMapper.countTrend(LocalDate.now().minusDays(6)));
    }

    /** 各科室预约量分布 */
    public List<Map<String, Object>> departmentDistribution() {
        return appointmentMapper.countByDepartment();
    }

    /** 医生接诊量 TOP10 */
    public List<Map<String, Object>> doctorWorkload() {
        return appointmentMapper.doctorWorkload();
    }

    /** 药品消耗 TOP10 */
    public List<Map<String, Object>> medicationConsumption() {
        return prescriptionItemMapper.medicationConsumption();
    }

    /**
     * 医生工作台数据：今日接诊、待就诊、累计完成、患者评分
     */
    public Map<String, Object> doctorOverview(Long doctorUserId) {
        Map<String, Object> map = new HashMap<>();
        LocalDate today = LocalDate.now();
        // 资质状态（前端据此提示"未审核不可出诊"）
        DoctorInfo self = doctorInfoMapper.selectOne(
                new LambdaQueryWrapper<DoctorInfo>().eq(DoctorInfo::getUserId, doctorUserId));
        map.put("auditStatus", self == null ? 0 : self.getAuditStatus());
        map.put("auditRemark", self == null ? null : self.getAuditRemark());
        // 今日待接诊
        map.put("todayAppointments", appointmentMapper.selectCount(new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getDoctorId, doctorUserId)
                .eq(Appointment::getAppointDate, today)
                .eq(Appointment::getStatus, Appointment.STATUS_ASSIGNED)));
        // 待就诊总数（已派单、含今天之后）
        map.put("upcomingAppointments", appointmentMapper.selectCount(new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getDoctorId, doctorUserId)
                .eq(Appointment::getStatus, Appointment.STATUS_ASSIGNED)
                .ge(Appointment::getAppointDate, today)));
        // 累计完成接诊
        map.put("completedTotal", appointmentMapper.selectCount(new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getDoctorId, doctorUserId)
                .eq(Appointment::getStatus, Appointment.STATUS_COMPLETED)));
        // 待写病历（已完成就诊但未写病历的今日之前预约）
        map.put("prescriptionTotal", prescriptionMapper.selectCount(new LambdaQueryWrapper<Prescription>()
                .eq(Prescription::getDoctorId, doctorUserId)));
        // 患者评分
        List<Evaluation> evaluations = evaluationMapper.selectList(
                new LambdaQueryWrapper<Evaluation>().eq(Evaluation::getDoctorId, doctorUserId));
        map.put("evaluationCount", evaluations.size());
        double avg = evaluations.stream().mapToInt(Evaluation::getScore).average().orElse(0);
        map.put("avgScore", Math.round(avg * 10) / 10.0);
        return map;
    }

    /** 医生近7天接诊趋势（按就诊日期，补零） */
    public List<Map<String, Object>> doctorTrend(Long doctorUserId) {
        return fillZero(appointmentMapper.doctorTrend(doctorUserId, LocalDate.now().minusDays(6)));
    }

    /** 将 SQL 返回的 (label,value) 按最近7天补零对齐，缺失日期计 0 */
    private List<Map<String, Object>> fillZero(List<Map<String, Object>> rows) {
        Map<String, Object> byDate = new HashMap<>();
        for (Map<String, Object> row : rows) {
            byDate.put(String.valueOf(row.get("label")), row.get("value"));
        }
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            String date = LocalDate.now().minusDays(i).toString();
            Map<String, Object> item = new HashMap<>();
            item.put("label", date);
            Object value = byDate.get(date);
            item.put("value", value instanceof Number number ? number.intValue() : 0);
            result.add(item);
        }
        return result;
    }

    /**
     * 患者首页数据：我的预约、已完成就诊、待缴费处方、病历数、最近一次就诊安排
     */
    public Map<String, Object> patientOverview(Long patientId) {
        Map<String, Object> map = new HashMap<>();
        map.put("appointmentTotal", appointmentMapper.selectCount(new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getPatientId, patientId)));
        map.put("completedVisits", appointmentMapper.selectCount(new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getPatientId, patientId)
                .eq(Appointment::getStatus, Appointment.STATUS_COMPLETED)));
        map.put("unpaidPrescriptions", prescriptionMapper.selectCount(new LambdaQueryWrapper<Prescription>()
                .eq(Prescription::getPatientId, patientId)
                .eq(Prescription::getStatus, Prescription.STATUS_UNPAID)));
        map.put("recordTotal", medicalRecordMapper.selectCount(new LambdaQueryWrapper<MedicalRecord>()
                .eq(MedicalRecord::getPatientId, patientId)));
        // 最近一次就诊安排（已派单、今天及之后，按日期最近）
        Appointment next = appointmentMapper.selectOne(new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getPatientId, patientId)
                .eq(Appointment::getStatus, Appointment.STATUS_ASSIGNED)
                .ge(Appointment::getAppointDate, LocalDate.now())
                .orderByAsc(Appointment::getAppointDate)
                .last("LIMIT 1"));
        map.put("nextAppointment", next);
        return map;
    }
}
