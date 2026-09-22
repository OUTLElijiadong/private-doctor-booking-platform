package com.doctor.appointment.modules.medication.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.doctor.appointment.common.exception.BizException;
import com.doctor.appointment.modules.medication.entity.Medication;
import com.doctor.appointment.modules.medication.entity.MedicationCategory;
import com.doctor.appointment.modules.medication.entity.MedicationStockLog;
import com.doctor.appointment.modules.medication.entity.Supplier;
import com.doctor.appointment.modules.medication.mapper.MedicationCategoryMapper;
import com.doctor.appointment.modules.medication.mapper.MedicationMapper;
import com.doctor.appointment.modules.medication.mapper.MedicationStockLogMapper;
import com.doctor.appointment.modules.medication.mapper.SupplierMapper;
import com.doctor.appointment.modules.user.entity.SysUser;
import com.doctor.appointment.modules.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 药品服务：药品维护、库存入库/调整、出入库流水查询
 */
@Service
@RequiredArgsConstructor
public class MedicationService extends ServiceImpl<MedicationMapper, Medication> {

    private final MedicationMapper medicationMapper;
    private final MedicationCategoryMapper categoryMapper;
    private final SupplierMapper supplierMapper;
    private final MedicationStockLogMapper stockLogMapper;
    private final SysUserMapper sysUserMapper;

    /** 药品分页（含分类名、供应商名） */
    public Page<Medication> pageMedications(int pageNum, int pageSize, String keyword, Long categoryId) {
        LambdaQueryWrapper<Medication> wrapper = new LambdaQueryWrapper<Medication>()
                .like(StringUtils.hasText(keyword), Medication::getName, keyword)
                .eq(categoryId != null, Medication::getCategoryId, categoryId)
                .orderByDesc(Medication::getCreatedAt);
        Page<Medication> page = page(new Page<>(pageNum, pageSize), wrapper);
        fillExtra(page.getRecords());
        return page;
    }

    /** 在售药品列表（开处方下拉用） */
    public List<Medication> listOnSale() {
        List<Medication> list = list(new LambdaQueryWrapper<Medication>()
                .eq(Medication::getStatus, 1)
                .orderByAsc(Medication::getName));
        fillExtra(list);
        return list;
    }

    /**
     * 库存变动：type=IN 入库 / ADJUST 人工调整（quantity 可为负）。
     * 原子扣减，并发下不会出现负库存。
     */
    @Transactional(rollbackFor = Exception.class)
    public void changeStock(Long medicationId, Integer quantity, String refType, String remark, Long operatorId) {
        Medication medication = getById(medicationId);
        if (medication == null) {
            throw new BizException(400, "药品不存在");
        }
        if (medicationMapper.adjustStock(medicationId, quantity) == 0) {
            throw new BizException(400, "调整后库存不能为负数，当前库存 " + medication.getStock());
        }
        MedicationStockLog log = new MedicationStockLog();
        log.setMedicationId(medicationId);
        log.setType(quantity >= 0 ? "IN" : "OUT");
        log.setQuantity(Math.abs(quantity));
        log.setBeforeStock(medication.getStock());
        log.setAfterStock(medication.getStock() + quantity);
        log.setRefType(refType);
        log.setRemark(remark);
        log.setCreatedBy(operatorId);
        stockLogMapper.insert(log);
    }

    /** 出入库流水分页 */
    public Page<MedicationStockLog> pageStockLogs(int pageNum, int pageSize, Long medicationId, String type) {
        LambdaQueryWrapper<MedicationStockLog> wrapper = new LambdaQueryWrapper<MedicationStockLog>()
                .eq(medicationId != null, MedicationStockLog::getMedicationId, medicationId)
                .eq(StringUtils.hasText(type), MedicationStockLog::getType, type)
                .orderByDesc(MedicationStockLog::getCreatedAt);
        Page<MedicationStockLog> page = stockLogMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<MedicationStockLog> records = page.getRecords();
        if (!records.isEmpty()) {
            List<Long> medIds = records.stream().map(MedicationStockLog::getMedicationId).distinct().collect(Collectors.toList());
            Map<Long, String> medMap = medicationMapper.selectBatchIds(medIds).stream()
                    .collect(Collectors.toMap(Medication::getId, Medication::getName));
            List<Long> operatorIds = records.stream().map(MedicationStockLog::getCreatedBy)
                    .filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
            // HashMap 容忍 createdBy 为 null 的流水（get(null) 在 Map.of() 上会 NPE）
            Map<Long, String> userMap = new java.util.HashMap<>();
            if (!operatorIds.isEmpty()) {
                sysUserMapper.selectBatchIds(operatorIds)
                        .forEach(u -> userMap.put(u.getId(), u.getRealName()));
            }
            for (MedicationStockLog log : records) {
                log.setMedicationName(medMap.get(log.getMedicationId()));
                log.setOperatorName(userMap.get(log.getCreatedBy()));
            }
        }
        return page;
    }

    private void fillExtra(List<Medication> list) {
        if (list.isEmpty()) {
            return;
        }
        List<Long> categoryIds = list.stream().map(Medication::getCategoryId)
                .filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        // 用 HashMap 而非 Map.of()：不可变空 Map 对 get(null) 会抛 NPE（分类/供应商可空）
        Map<Long, String> categoryMap = new java.util.HashMap<>();
        if (!categoryIds.isEmpty()) {
            categoryMapper.selectBatchIds(categoryIds)
                    .forEach(c -> categoryMap.put(c.getId(), c.getName()));
        }
        List<Long> supplierIds = list.stream().map(Medication::getSupplierId)
                .filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, String> supplierMap = new java.util.HashMap<>();
        if (!supplierIds.isEmpty()) {
            supplierMapper.selectBatchIds(supplierIds)
                    .forEach(sp -> supplierMap.put(sp.getId(), sp.getName()));
        }
        for (Medication m : list) {
            m.setCategoryName(categoryMap.get(m.getCategoryId()));
            m.setSupplierName(supplierMap.get(m.getSupplierId()));
        }
    }
}
