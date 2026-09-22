package com.doctor.appointment.modules.prescription.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.doctor.appointment.modules.prescription.entity.PrescriptionItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface PrescriptionItemMapper extends BaseMapper<PrescriptionItem> {

    /** 药品消耗 TOP10（按已发药处方的开药数量汇总） */
    @Select("SELECT i.medication_name AS label, SUM(i.quantity) AS value FROM prescription_item i " +
            "JOIN prescription p ON i.prescription_id = p.id WHERE p.status = 2 " +
            "GROUP BY i.medication_name ORDER BY value DESC LIMIT 10")
    List<Map<String, Object>> medicationConsumption();
}
