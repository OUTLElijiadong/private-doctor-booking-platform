package com.doctor.appointment.modules.prescription.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.doctor.appointment.common.exception.BizException;
import com.doctor.appointment.modules.appointment.entity.MedicalRecord;
import com.doctor.appointment.modules.appointment.mapper.MedicalRecordMapper;
import com.doctor.appointment.modules.medication.entity.Medication;
import com.doctor.appointment.modules.medication.entity.MedicationStockLog;
import com.doctor.appointment.modules.medication.mapper.MedicationMapper;
import com.doctor.appointment.modules.medication.mapper.MedicationStockLogMapper;
import com.doctor.appointment.modules.prescription.dto.PrescriptionCreateDTO;
import com.doctor.appointment.modules.prescription.entity.Prescription;
import com.doctor.appointment.modules.prescription.entity.PrescriptionItem;
import com.doctor.appointment.modules.prescription.mapper.PrescriptionItemMapper;
import com.doctor.appointment.modules.prescription.mapper.PrescriptionMapper;
import com.doctor.appointment.modules.user.entity.SysUser;
import com.doctor.appointment.modules.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 处方服务：开立 → 缴费 → 发药（联动库存出库）→ 作废，全流程状态管控
 */
@Service
@RequiredArgsConstructor
public class PrescriptionService extends ServiceImpl<PrescriptionMapper, Prescription> {

    private final PrescriptionMapper prescriptionMapper;
    private final PrescriptionItemMapper itemMapper;
    private final MedicalRecordMapper medicalRecordMapper;
    private final MedicationMapper medicationMapper;
    private final MedicationStockLogMapper stockLogMapper;
    private final SysUserMapper sysUserMapper;

    /** 医生基于病历开具处方（一处方对应一病历，含明细与金额计算） */
    @Transactional(rollbackFor = Exception.class)
    public Prescription create(Long doctorUserId, PrescriptionCreateDTO dto) {
        MedicalRecord record = medicalRecordMapper.selectById(dto.getRecordId());
        if (record == null) {
            throw new BizException(400, "病历不存在");
        }
        if (!doctorUserId.equals(record.getDoctorId())) {
            throw new BizException(403, "只能为自己的病历开具处方");
        }
        long exists = count(new LambdaQueryWrapper<Prescription>().eq(Prescription::getRecordId, dto.getRecordId()));
        if (exists > 0) {
            throw new BizException(400, "该病历已开具处方");
        }
        Prescription prescription = new Prescription();
        prescription.setPrescriptionNo("RX" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + System.currentTimeMillis() % 100_000
                + ThreadLocalRandom.current().nextInt(100, 999));
        prescription.setRecordId(record.getId());
        prescription.setPatientId(record.getPatientId());
        prescription.setDoctorId(doctorUserId);
        prescription.setDiagnosis(dto.getDiagnosis() != null ? dto.getDiagnosis() : record.getDiagnosis());
        prescription.setAdvice(dto.getAdvice());
        prescription.setStatus(Prescription.STATUS_UNPAID);

        // 明细与金额
        BigDecimal total = BigDecimal.ZERO;
        List<PrescriptionItem> items = new java.util.ArrayList<>();
        for (PrescriptionCreateDTO.Item itemDto : dto.getItems()) {
            Medication medication = medicationMapper.selectById(itemDto.getMedicationId());
            if (medication == null || medication.getStatus() != 1) {
                throw new BizException(400, "药品不存在或已下架，ID=" + itemDto.getMedicationId());
            }
            if (itemDto.getQuantity() == null || itemDto.getQuantity() <= 0) {
                throw new BizException(400, "药品数量必须大于0");
            }
            PrescriptionItem item = new PrescriptionItem();
            item.setMedicationId(medication.getId());
            item.setMedicationName(medication.getName());
            item.setDosage(itemDto.getDosage());
            item.setUsageNote(itemDto.getUsageNote());
            item.setQuantity(itemDto.getQuantity());
            item.setUnitPrice(medication.getPrice());
            item.setSubtotal(medication.getPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity())));
            total = total.add(item.getSubtotal());
            items.add(item);
        }
        prescription.setTotalAmount(total);
        save(prescription);
        for (PrescriptionItem item : items) {
            item.setPrescriptionId(prescription.getId());
            itemMapper.insert(item);
        }
        return prescription;
    }

    /** 患者缴费（模拟支付） */
    public void pay(Long id, Long patientId) {
        Prescription prescription = getById(id);
        if (prescription == null || !prescription.getPatientId().equals(patientId)) {
            throw new BizException(400, "处方不存在");
        }
        if (prescription.getStatus() != Prescription.STATUS_UNPAID) {
            throw new BizException(400, "当前状态不可缴费");
        }
        prescription.setStatus(Prescription.STATUS_PAID);
        updateById(prescription);
    }

    /**
     * 发药：原子扣减库存并逐笔写出库记录（事务保证一致性，并发不会超卖）
     */
    @Transactional(rollbackFor = Exception.class)
    public void dispense(Long id, Long operatorId) {
        Prescription prescription = getById(id);
        if (prescription == null) {
            throw new BizException(400, "处方不存在");
        }
        if (prescription.getStatus() != Prescription.STATUS_PAID) {
            throw new BizException(400, "仅已缴费的处方可以发药");
        }
        List<PrescriptionItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<PrescriptionItem>().eq(PrescriptionItem::getPrescriptionId, id));
        for (PrescriptionItem item : items) {
            Medication medication = medicationMapper.selectById(item.getMedicationId());
            if (medication == null) {
                throw new BizException(400, "药品不存在：" + item.getMedicationName());
            }
            // 原子扣减：库存不足直接失败回滚
            if (medicationMapper.adjustStock(medication.getId(), -item.getQuantity()) == 0) {
                throw new BizException(400, "药品【" + medication.getName() + "】库存不足，当前库存 " + medication.getStock());
            }
            // 出库记录
            MedicationStockLog log = new MedicationStockLog();
            log.setMedicationId(medication.getId());
            log.setType("OUT");
            log.setQuantity(item.getQuantity());
            log.setBeforeStock(medication.getStock());
            log.setAfterStock(medication.getStock() - item.getQuantity());
            log.setRefType("PRESCRIPTION");
            log.setRefId(id);
            log.setRemark("处方发药：" + prescription.getPrescriptionNo());
            log.setCreatedBy(operatorId);
            stockLogMapper.insert(log);
        }
        prescription.setStatus(Prescription.STATUS_DISPENSED);
        updateById(prescription);
    }

    /** 作废处方（仅未发药前可操作） */
    public void cancel(Long id, Long userId, String role) {
        Prescription prescription = getById(id);
        if (prescription == null) {
            throw new BizException(400, "处方不存在");
        }
        if ("DOCTOR".equals(role) && !prescription.getDoctorId().equals(userId)) {
            throw new BizException(403, "只能作废自己开具的处方");
        }
        if (prescription.getStatus() == Prescription.STATUS_DISPENSED) {
            throw new BizException(400, "已发药的处方不可作废");
        }
        if (prescription.getStatus() == Prescription.STATUS_VOID) {
            throw new BizException(400, "处方已作废");
        }
        prescription.setStatus(Prescription.STATUS_VOID);
        updateById(prescription);
    }

    /** 分页查询（患者看自己的，医生看开的，管理员看全部），附带明细与姓名 */
    public Page<Prescription> pagePrescriptions(int pageNum, int pageSize, Integer status,
                                                String role, Long userId) {
        LambdaQueryWrapper<Prescription> wrapper = new LambdaQueryWrapper<Prescription>()
                .eq(status != null, Prescription::getStatus, status)
                .orderByDesc(Prescription::getCreatedAt);
        if ("PATIENT".equals(role)) {
            wrapper.eq(Prescription::getPatientId, userId);
        } else if ("DOCTOR".equals(role)) {
            wrapper.eq(Prescription::getDoctorId, userId);
        }
        Page<Prescription> page = page(new Page<>(pageNum, pageSize), wrapper);
        fillExtra(page.getRecords());
        return page;
    }

    /** 处方详情（按角色隔离：患者仅自己的，医生仅自己开具的，管理员全部） */
    public Prescription detail(Long id, String role, Long userId) {
        Prescription prescription = getById(id);
        if (prescription == null) {
            return null;
        }
        if ("PATIENT".equals(role) && !prescription.getPatientId().equals(userId)) {
            throw new BizException(403, "无权查看该处方");
        }
        if ("DOCTOR".equals(role) && !prescription.getDoctorId().equals(userId)) {
            throw new BizException(403, "无权查看该处方");
        }
        fillExtra(List.of(prescription));
        return prescription;
    }

    private void fillExtra(List<Prescription> list) {
        if (list.isEmpty()) {
            return;
        }
        List<Long> userIds = list.stream()
                .flatMap(p -> java.util.stream.Stream.of(p.getPatientId(), p.getDoctorId()))
                .distinct().collect(Collectors.toList());
        Map<Long, String> nameMap = sysUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getRealName));
        for (Prescription p : list) {
            p.setPatientName(nameMap.get(p.getPatientId()));
            p.setDoctorName(nameMap.get(p.getDoctorId()));
            p.setItems(itemMapper.selectList(
                    new LambdaQueryWrapper<PrescriptionItem>().eq(PrescriptionItem::getPrescriptionId, p.getId())));
        }
    }
}
