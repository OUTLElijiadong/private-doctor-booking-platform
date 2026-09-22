package com.doctor.appointment.modules.prescription.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.doctor.appointment.modules.prescription.entity.Prescription;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PrescriptionMapper extends BaseMapper<Prescription> {
}
