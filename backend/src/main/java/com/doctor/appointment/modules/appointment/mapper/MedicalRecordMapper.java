package com.doctor.appointment.modules.appointment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.doctor.appointment.modules.appointment.entity.MedicalRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MedicalRecordMapper extends BaseMapper<MedicalRecord> {
}
