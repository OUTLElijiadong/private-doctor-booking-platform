package com.doctor.appointment.modules.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.doctor.appointment.modules.department.entity.Department;
import com.doctor.appointment.modules.department.mapper.DepartmentMapper;
import com.doctor.appointment.modules.user.entity.DoctorInfo;
import com.doctor.appointment.modules.user.entity.Evaluation;
import com.doctor.appointment.modules.user.entity.SysUser;
import com.doctor.appointment.modules.user.mapper.DoctorInfoMapper;
import com.doctor.appointment.modules.user.mapper.EvaluationMapper;
import com.doctor.appointment.modules.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 医生信息服务（含姓名、科室、评分等关联信息填充）
 */
@Service
@RequiredArgsConstructor
public class DoctorService extends ServiceImpl<DoctorInfoMapper, DoctorInfo> {

    private final SysUserMapper sysUserMapper;
    private final DepartmentMapper departmentMapper;
    private final EvaluationMapper evaluationMapper;

    /**
     * 分页查询医生（患者端仅看资质已通过的医生；关键字在 SQL 层过滤，分页正确）
     */
    public Page<DoctorInfo> pageDoctors(int pageNum, int pageSize, Long departmentId, String keyword, boolean onlyApproved) {
        LambdaQueryWrapper<DoctorInfo> wrapper = new LambdaQueryWrapper<DoctorInfo>()
                .eq(departmentId != null, DoctorInfo::getDepartmentId, departmentId)
                .eq(onlyApproved, DoctorInfo::getAuditStatus, 2)
                .orderByDesc(DoctorInfo::getCreatedAt);
        if (keyword != null && !keyword.isBlank()) {
            List<Long> matchedUserIds = sysUserMapper.selectList(
                            new LambdaQueryWrapper<SysUser>().like(SysUser::getRealName, keyword))
                    .stream().map(SysUser::getId).collect(Collectors.toList());
            if (matchedUserIds.isEmpty()) {
                return new Page<>(pageNum, pageSize);
            }
            wrapper.in(DoctorInfo::getUserId, matchedUserIds);
        }
        Page<DoctorInfo> page = page(new Page<>(pageNum, pageSize), wrapper);
        fillExtra(page.getRecords(), null);
        return page;
    }

    public DoctorInfo getByUserId(Long userId) {
        return getOne(new LambdaQueryWrapper<DoctorInfo>().eq(DoctorInfo::getUserId, userId));
    }

    public DoctorInfo detail(Long id) {
        DoctorInfo info = getById(id);
        if (info != null) {
            fillExtra(List.of(info), null);
        }
        return info;
    }

    /** 填充姓名、科室、头像、平均评分（评分批量查询，避免 N+1） */
    private void fillExtra(List<DoctorInfo> list, String keyword) {
        if (list.isEmpty()) {
            return;
        }
        List<Long> userIds = list.stream().map(DoctorInfo::getUserId).collect(Collectors.toList());
        Map<Long, SysUser> userMap = sysUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u));
        List<Long> deptIds = list.stream().map(DoctorInfo::getDepartmentId).filter(java.util.Objects::nonNull).collect(Collectors.toList());
        // HashMap 容忍 departmentId 为 null 的医生（未分科室）
        Map<Long, String> deptMap = new HashMap<>();
        if (!deptIds.isEmpty()) {
            departmentMapper.selectBatchIds(deptIds).forEach(d -> deptMap.put(d.getId(), d.getName()));
        }
        // 一次取出全部相关评价再按医生分组
        Map<Long, List<Evaluation>> evalMap = evaluationMapper.selectList(
                        new LambdaQueryWrapper<Evaluation>().in(Evaluation::getDoctorId, userIds))
                .stream().collect(Collectors.groupingBy(Evaluation::getDoctorId));
        for (DoctorInfo info : list) {
            SysUser user = userMap.get(info.getUserId());
            if (user != null) {
                info.setRealName(user.getRealName());
                info.setAvatar(user.getAvatar());
            }
            info.setDepartmentName(deptMap.get(info.getDepartmentId()));
            List<Evaluation> evals = evalMap.get(info.getUserId());
            if (evals != null && !evals.isEmpty()) {
                double avg = evals.stream().mapToInt(Evaluation::getScore).average().orElse(0);
                info.setAvgScore(Math.round(avg * 10) / 10.0);
            }
        }
    }
}
