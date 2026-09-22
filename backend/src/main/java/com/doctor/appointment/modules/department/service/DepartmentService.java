package com.doctor.appointment.modules.department.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.doctor.appointment.modules.department.entity.Department;
import com.doctor.appointment.modules.department.mapper.DepartmentMapper;
import com.doctor.appointment.modules.user.entity.DoctorInfo;
import com.doctor.appointment.modules.user.mapper.DoctorInfoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 科室服务
 */
@Service
@RequiredArgsConstructor
public class DepartmentService extends ServiceImpl<DepartmentMapper, Department> {

    private final DoctorInfoMapper doctorInfoMapper;

    /** 统计某科室下的在册医生数（删除科室前校验用） */
    public long countDoctors(Long departmentId) {
        return doctorInfoMapper.selectCount(new LambdaQueryWrapper<DoctorInfo>()
                .eq(DoctorInfo::getDepartmentId, departmentId));
    }

    /** 启用状态的科室列表（患者端选科室用） */
    public List<Department> listEnabled() {
        return list(new LambdaQueryWrapper<Department>()
                .eq(Department::getStatus, 1)
                .orderByAsc(Department::getSort));
    }
}
