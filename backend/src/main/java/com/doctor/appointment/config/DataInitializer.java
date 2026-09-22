package com.doctor.appointment.config;

import com.doctor.appointment.modules.department.entity.Department;
import com.doctor.appointment.modules.department.mapper.DepartmentMapper;
import com.doctor.appointment.modules.medication.entity.MedicationCategory;
import com.doctor.appointment.modules.medication.mapper.MedicationCategoryMapper;
import com.doctor.appointment.modules.user.entity.DoctorInfo;
import com.doctor.appointment.modules.user.entity.SysUser;
import com.doctor.appointment.modules.user.mapper.DoctorInfoMapper;
import com.doctor.appointment.modules.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 数据初始化：首次启动时创建初始账号与基础数据（初始密码均为 123456，登录后请及时修改）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final SysUserMapper sysUserMapper;
    private final DoctorInfoMapper doctorInfoMapper;
    private final DepartmentMapper departmentMapper;
    private final MedicationCategoryMapper categoryMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void run(ApplicationArguments args) {
        initUsers();
        initDepartments();
        initMedicationCategories();
    }

    private void initUsers() {
        if (sysUserMapper.selectCount(null) > 0) {
            return;
        }
        createUser("admin", "系统管理员", "ADMIN");
        SysUser doctor = createUser("zhoujianguo", "周建国", "DOCTOR");
        createUser("lihuimin", "李慧敏", "PATIENT");
        // 内科主治医师初始档案，资质已通过，患者端可直接预约
        DoctorInfo info = new DoctorInfo();
        info.setUserId(doctor.getId());
        info.setDepartmentId(1L);
        info.setTitle("主治医师");
        info.setSpecialty("内科常见病、多发病诊治");
        info.setIntroduction("从事内科临床工作多年，擅长内科常见病诊疗。");
        info.setYears(10);
        info.setAuditStatus(2);
        doctorInfoMapper.insert(info);
        log.info("已创建初始账号：admin / zhoujianguo / lihuimin，初始密码均为 123456");
    }

    private SysUser createUser(String username, String realName, String role) {
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode("123456"));
        user.setRealName(realName);
        user.setRole(role);
        user.setStatus(1);
        sysUserMapper.insert(user);
        return user;
    }

    private void initDepartments() {
        if (departmentMapper.selectCount(null) > 0) {
            return;
        }
        List<String> names = List.of("内科", "外科", "儿科", "妇科", "眼科", "口腔科", "皮肤科", "中医科");
        int sort = 1;
        for (String name : names) {
            Department dept = new Department();
            dept.setName(name);
            dept.setSort(sort++);
            dept.setStatus(1);
            departmentMapper.insert(dept);
        }
        log.info("已初始化 {} 个默认科室", names.size());
    }

    private void initMedicationCategories() {
        if (categoryMapper.selectCount(null) > 0) {
            return;
        }
        List<String> names = List.of("感冒用药", "消炎镇痛", "心脑血管", "消化系统", "外用药品", "维生素类");
        for (String name : names) {
            MedicationCategory category = new MedicationCategory();
            category.setName(name);
            categoryMapper.insert(category);
        }
        log.info("已初始化 {} 个默认药品分类", names.size());
    }
}
