package com.doctor.appointment.modules.department.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.doctor.appointment.modules.department.entity.Department;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DepartmentMapper extends BaseMapper<Department> {
}
