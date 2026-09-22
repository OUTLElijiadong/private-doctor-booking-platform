package com.doctor.appointment.modules.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.doctor.appointment.common.exception.BizException;
import com.doctor.appointment.modules.appointment.entity.Appointment;
import com.doctor.appointment.modules.appointment.mapper.AppointmentMapper;
import com.doctor.appointment.modules.user.entity.Evaluation;
import com.doctor.appointment.modules.user.entity.SysUser;
import com.doctor.appointment.modules.user.mapper.EvaluationMapper;
import com.doctor.appointment.modules.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 医德评价服务
 */
@Service
@RequiredArgsConstructor
public class EvaluationService extends ServiceImpl<EvaluationMapper, Evaluation> {

    private final AppointmentMapper appointmentMapper;
    private final SysUserMapper sysUserMapper;

    /** 患者对已完成的就诊进行评价（一次就诊仅一次） */
    public void create(Long patientId, Evaluation evaluation) {
        Appointment appointment = appointmentMapper.selectById(evaluation.getAppointmentId());
        if (appointment == null || !appointment.getPatientId().equals(patientId)) {
            throw new BizException(400, "预约记录不存在");
        }
        if (appointment.getStatus() != Appointment.STATUS_COMPLETED) {
            throw new BizException(400, "就诊完成后才能评价");
        }
        long exists = count(new LambdaQueryWrapper<Evaluation>()
                .eq(Evaluation::getAppointmentId, evaluation.getAppointmentId()));
        if (exists > 0) {
            throw new BizException(400, "该次就诊已评价过");
        }
        if (evaluation.getScore() == null || evaluation.getScore() < 1 || evaluation.getScore() > 5) {
            throw new BizException(400, "评分需在1-5之间");
        }
        evaluation.setId(null);
        evaluation.setPatientId(patientId);
        evaluation.setDoctorId(appointment.getDoctorId());
        save(evaluation);
    }

    /** 回复评价（医生本人或管理员） */
    public void reply(Long id, String reply, Long userId, boolean isAdmin) {
        Evaluation evaluation = getById(id);
        if (evaluation == null) {
            throw new BizException(400, "评价不存在");
        }
        if (!isAdmin && !evaluation.getDoctorId().equals(userId)) {
            throw new BizException(403, "只能回复自己的评价");
        }
        evaluation.setReply(reply);
        updateById(evaluation);
    }

    /** 分页查询：患者看自己的，医生看收到的（评价者姓名脱敏），管理员看全部 */
    public Page<Evaluation> pageEvaluations(int pageNum, int pageSize, Long doctorId,
                                            Long userId, String role) {
        LambdaQueryWrapper<Evaluation> wrapper = new LambdaQueryWrapper<Evaluation>()
                .eq(doctorId != null, Evaluation::getDoctorId, doctorId)
                .orderByDesc(Evaluation::getCreatedAt);
        if ("PATIENT".equals(role)) {
            wrapper.eq(Evaluation::getPatientId, userId);
        } else if ("DOCTOR".equals(role)) {
            wrapper.eq(Evaluation::getDoctorId, userId);
        }
        Page<Evaluation> page = page(new Page<>(pageNum, pageSize), wrapper);
        fillNames(page.getRecords(), role);
        return page;
    }

    private void fillNames(List<Evaluation> list, String role) {
        if (list.isEmpty()) {
            return;
        }
        List<Long> userIds = list.stream()
                .flatMap(e -> java.util.stream.Stream.of(e.getPatientId(), e.getDoctorId()))
                .distinct().collect(Collectors.toList());
        Map<Long, String> nameMap = sysUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getRealName));
        for (Evaluation e : list) {
            e.setPatientName(nameMap.get(e.getPatientId()));
            e.setDoctorName(nameMap.get(e.getDoctorId()));
            // 医生视角脱敏评价者姓名（保留姓氏）：依据《民法典》第1226条患者隐私保密义务
            // 与《个人信息保护法》第28/73条敏感个人信息去标识化要求；管理员管理场景保留全名
            if ("DOCTOR".equals(role) && e.getPatientName() != null) {
                e.setPatientName(desensitize(e.getPatientName()));
            }
        }
    }

    /** 姓名去标识化：保留姓氏，其余以 * 代替（如 王思远 → 王**） */
    private String desensitize(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        return name.charAt(0) + "**";
    }
}
