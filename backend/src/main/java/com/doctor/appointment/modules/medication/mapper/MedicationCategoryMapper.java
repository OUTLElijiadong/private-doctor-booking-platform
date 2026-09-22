package com.doctor.appointment.modules.medication.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.doctor.appointment.modules.medication.entity.MedicationCategory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MedicationCategoryMapper extends BaseMapper<MedicationCategory> {
}
