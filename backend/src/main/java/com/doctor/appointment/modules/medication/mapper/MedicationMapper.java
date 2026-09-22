package com.doctor.appointment.modules.medication.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.doctor.appointment.modules.medication.entity.Medication;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MedicationMapper extends BaseMapper<Medication> {

    /**
     * 原子调整库存：stock = stock + delta，调整后不能为负，成功返回 1，库存不足返回 0
     */
    @Update("UPDATE medication SET stock = stock + #{delta} " +
            "WHERE id = #{id} AND stock + #{delta} >= 0")
    int adjustStock(@Param("id") Long id, @Param("delta") int delta);
}
