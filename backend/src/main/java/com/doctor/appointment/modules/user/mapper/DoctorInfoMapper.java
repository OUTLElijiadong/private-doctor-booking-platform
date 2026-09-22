package com.doctor.appointment.modules.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.doctor.appointment.modules.user.entity.DoctorInfo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DoctorInfoMapper extends BaseMapper<DoctorInfo> {
}
