package com.doctor.appointment.modules.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.doctor.appointment.common.exception.BizException;
import com.doctor.appointment.modules.user.dto.AuditDTO;
import com.doctor.appointment.modules.user.dto.ReviewSubmitDTO;
import com.doctor.appointment.modules.user.entity.DoctorInfo;
import com.doctor.appointment.modules.user.entity.DoctorReview;
import com.doctor.appointment.modules.user.entity.SysUser;
import com.doctor.appointment.modules.user.mapper.DoctorInfoMapper;
import com.doctor.appointment.modules.user.mapper.DoctorReviewMapper;
import com.doctor.appointment.modules.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 医生执业资格审核服务
 */
@Service
@RequiredArgsConstructor
public class DoctorReviewService extends ServiceImpl<DoctorReviewMapper, DoctorReview> {

    private final DoctorInfoMapper doctorInfoMapper;
    private final SysUserMapper sysUserMapper;

    /** 医生提交资质材料 */
    @Transactional(rollbackFor = Exception.class)
    public void submit(Long userId, ReviewSubmitDTO dto) {
        DoctorInfo info = doctorInfoMapper.selectOne(
                new LambdaQueryWrapper<DoctorInfo>().eq(DoctorInfo::getUserId, userId));
        if (info == null) {
            throw new BizException(400, "医生信息不存在");
        }
        if (info.getAuditStatus() != null && info.getAuditStatus() == 1) {
            throw new BizException(400, "已有待审核的资质材料，请耐心等待");
        }
        DoctorReview review = new DoctorReview();
        review.setDoctorId(info.getId());
        review.setCertName(dto.getCertName());
        review.setCertNo(dto.getCertNo());
        review.setMaterialUrl(dto.getMaterialUrl());
        review.setStatus(0);
        save(review);
        // 医生状态置为待审核
        info.setAuditStatus(1);
        doctorInfoMapper.updateById(info);
    }

    /** 管理员审核 */
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long adminId, AuditDTO dto) {
        if (dto.getStatus() == null || (dto.getStatus() != 1 && dto.getStatus() != 2)) {
            throw new BizException(400, "审核状态参数不合法");
        }
        DoctorReview review = getById(dto.getId());
        if (review == null) {
            throw new BizException(400, "审核记录不存在");
        }
        if (review.getStatus() != 0) {
            throw new BizException(400, "该记录已审核，请勿重复操作");
        }
        review.setStatus(dto.getStatus());
        review.setRemark(dto.getRemark());
        review.setAuditBy(adminId);
        review.setAuditTime(LocalDateTime.now());
        updateById(review);
        // 同步医生资质状态：1 通过 → 2 已通过；2 驳回 → 3 已驳回
        DoctorInfo info = doctorInfoMapper.selectById(review.getDoctorId());
        if (info != null) {
            info.setAuditStatus(dto.getStatus() == 1 ? 2 : 3);
            info.setAuditRemark(dto.getRemark());
            doctorInfoMapper.updateById(info);
        }
    }

    /** 分页查询（管理员看全部，医生看自己的） */
    public Page<DoctorReview> pageReviews(int pageNum, int pageSize, Integer status, Long userId, boolean isAdmin) {
        LambdaQueryWrapper<DoctorReview> wrapper = new LambdaQueryWrapper<DoctorReview>()
                .eq(status != null, DoctorReview::getStatus, status)
                .orderByAsc(DoctorReview::getStatus)
                .orderByDesc(DoctorReview::getCreatedAt);
        if (!isAdmin) {
            DoctorInfo info = doctorInfoMapper.selectOne(
                    new LambdaQueryWrapper<DoctorInfo>().eq(DoctorInfo::getUserId, userId));
            wrapper.eq(DoctorReview::getDoctorId, info == null ? -1 : info.getId());
        }
        Page<DoctorReview> page = page(new Page<>(pageNum, pageSize), wrapper);
        fillDoctorName(page.getRecords());
        return page;
    }

    private void fillDoctorName(List<DoctorReview> list) {
        if (list.isEmpty()) {
            return;
        }
        List<Long> doctorIds = list.stream().map(DoctorReview::getDoctorId).collect(Collectors.toList());
        Map<Long, Long> doctorUserMap = doctorInfoMapper.selectBatchIds(doctorIds).stream()
                .collect(Collectors.toMap(DoctorInfo::getId, DoctorInfo::getUserId));
        Map<Long, String> userNameMap = sysUserMapper.selectBatchIds(doctorUserMap.values()).stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getRealName));
        for (DoctorReview review : list) {
            Long uid = doctorUserMap.get(review.getDoctorId());
            review.setDoctorName(uid == null ? null : userNameMap.get(uid));
        }
    }
}
