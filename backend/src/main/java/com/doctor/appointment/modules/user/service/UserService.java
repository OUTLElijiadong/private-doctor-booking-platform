package com.doctor.appointment.modules.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.doctor.appointment.common.exception.BizException;
import com.doctor.appointment.modules.appointment.entity.Appointment;
import com.doctor.appointment.modules.appointment.entity.MedicalRecord;
import com.doctor.appointment.modules.appointment.mapper.AppointmentMapper;
import com.doctor.appointment.modules.appointment.mapper.MedicalRecordMapper;
import com.doctor.appointment.modules.prescription.entity.Prescription;
import com.doctor.appointment.modules.prescription.mapper.PrescriptionMapper;
import com.doctor.appointment.modules.schedule.entity.Schedule;
import com.doctor.appointment.modules.schedule.mapper.ScheduleMapper;
import com.doctor.appointment.modules.user.dto.LoginDTO;
import com.doctor.appointment.modules.user.dto.LoginVO;
import com.doctor.appointment.modules.user.dto.PasswordChangeDTO;
import com.doctor.appointment.modules.user.dto.RegisterDTO;
import com.doctor.appointment.modules.user.dto.ResetPasswordDTO;
import com.doctor.appointment.modules.user.entity.DoctorInfo;
import com.doctor.appointment.modules.user.entity.Evaluation;
import com.doctor.appointment.modules.user.entity.SysUser;
import com.doctor.appointment.modules.user.mapper.DoctorInfoMapper;
import com.doctor.appointment.modules.user.mapper.EvaluationMapper;
import com.doctor.appointment.modules.user.mapper.SysUserMapper;
import com.doctor.appointment.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 用户与认证服务
 */
@Service
@RequiredArgsConstructor
public class UserService extends ServiceImpl<SysUserMapper, SysUser> {

    private final JwtUtils jwtUtils;
    private final DoctorInfoMapper doctorInfoMapper;
    private final AppointmentMapper appointmentMapper;
    private final MedicalRecordMapper medicalRecordMapper;
    private final PrescriptionMapper prescriptionMapper;
    private final EvaluationMapper evaluationMapper;
    private final ScheduleMapper scheduleMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /** 登录：校验账号密码，签发 JWT */
    public LoginVO login(LoginDTO dto) {
        SysUser user = getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getUsername()));
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BizException(400, "账号或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException(403, "账号已被冻结，请联系管理员");
        }
        user.setLastLoginAt(LocalDateTime.now());
        updateById(user);
        String token = jwtUtils.createToken(user.getId(), user.getUsername(), user.getRole());
        return new LoginVO(token, user);
    }

    /**
     * 注册：默认患者角色；选择医生身份时同步创建医生档案（资质状态=未提交），
     * 登录后需在「资质审核」提交材料，管理员审核通过后方可排班接诊。
     */
    public void register(RegisterDTO dto) {
        long count = count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new BizException(400, "账号已存在");
        }
        // 注册入口只允许患者/医生两种身份，管理员账号严禁自助注册
        String role = "DOCTOR".equals(dto.getRole()) ? "DOCTOR" : "PATIENT";
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setGender(dto.getGender());
        user.setRole(role);
        user.setStatus(1);
        save(user);
        if ("DOCTOR".equals(role)) {
            DoctorInfo info = new DoctorInfo();
            info.setUserId(user.getId());
            info.setAuditStatus(0);
            doctorInfoMapper.insert(info);
        }
    }

    /** 找回密码：账号 + 预留手机号验证 */
    public void resetPassword(ResetPasswordDTO dto) {
        SysUser user = getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getUsername()));
        if (user == null || !dto.getPhone().equals(user.getPhone())) {
            throw new BizException(400, "账号与预留手机号不匹配");
        }
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        updateById(user);
    }

    /** 修改密码 */
    public void changePassword(Long userId, PasswordChangeDTO dto) {
        SysUser user = getById(userId);
        if (user == null) {
            throw new BizException(400, "用户不存在");
        }
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BizException(400, "原密码错误");
        }
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        updateById(user);
    }

    /** 管理员分页查询用户 */
    public Page<SysUser> pageUsers(int pageNum, int pageSize, String keyword, String role) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .and(StringUtils.hasText(keyword), w -> w
                        .like(SysUser::getUsername, keyword)
                        .or().like(SysUser::getRealName, keyword)
                        .or().like(SysUser::getPhone, keyword))
                .eq(StringUtils.hasText(role), SysUser::getRole, role)
                .orderByDesc(SysUser::getCreatedAt);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    /** 管理员新增用户（可创建医生账号并初始化医生信息） */
    public void createUser(SysUser user) {
        long count = count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, user.getUsername()));
        if (count > 0) {
            throw new BizException(400, "账号已存在");
        }
        user.setId(null);
        user.setPassword(passwordEncoder.encode(StringUtils.hasText(user.getPassword()) ? user.getPassword() : "123456"));
        if (!StringUtils.hasText(user.getRole())) {
            user.setRole("PATIENT");
        }
        user.setStatus(1);
        save(user);
        if ("DOCTOR".equals(user.getRole())) {
            DoctorInfo info = new DoctorInfo();
            info.setUserId(user.getId());
            info.setAuditStatus(0);
            doctorInfoMapper.insert(info);
        }
    }

    /** 校验密码是否匹配（供其他模块复用） */
    public String encode(String raw) {
        return passwordEncoder.encode(raw);
    }

    /**
     * 删除用户：存在业务数据（预约/病历/处方/评价/排班）时拒绝删除，避免产生孤儿数据
     */
    public void deleteWithCheck(Long id, Long operatorId) {
        if (id.equals(operatorId)) {
            throw new BizException(400, "不能删除当前登录账号");
        }
        SysUser user = getById(id);
        if (user == null) {
            return;
        }
        long related = appointmentMapper.selectCount(new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getPatientId, id).or().eq(Appointment::getDoctorId, id));
        if (related == 0) {
            related = medicalRecordMapper.selectCount(new LambdaQueryWrapper<MedicalRecord>()
                    .eq(MedicalRecord::getPatientId, id).or().eq(MedicalRecord::getDoctorId, id));
        }
        if (related == 0) {
            related = prescriptionMapper.selectCount(new LambdaQueryWrapper<Prescription>()
                    .eq(Prescription::getPatientId, id).or().eq(Prescription::getDoctorId, id));
        }
        if (related == 0) {
            related = evaluationMapper.selectCount(new LambdaQueryWrapper<Evaluation>()
                    .eq(Evaluation::getPatientId, id).or().eq(Evaluation::getDoctorId, id));
        }
        if (related == 0) {
            related = scheduleMapper.selectCount(new LambdaQueryWrapper<Schedule>()
                    .eq(Schedule::getDoctorId, id));
        }
        if (related > 0) {
            throw new BizException(400, "该用户名下存在 " + related
                    + " 条业务记录（预约/病历/处方/评价/排班），为保数据完整请改为冻结账号");
        }
        // 同步清理医生档案（无业务数据的医生才走到这里）
        if ("DOCTOR".equals(user.getRole())) {
            doctorInfoMapper.delete(new LambdaQueryWrapper<DoctorInfo>()
                    .eq(DoctorInfo::getUserId, id));
        }
        removeById(id);
    }
}
