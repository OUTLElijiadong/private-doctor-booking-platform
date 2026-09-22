package com.doctor.appointment.modules.system.log;

import com.doctor.appointment.modules.system.entity.OperationLog;
import com.doctor.appointment.modules.system.mapper.OperationLogMapper;
import com.doctor.appointment.security.LoginUser;
import com.doctor.appointment.security.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 操作日志切面：拦截 @LogOperation 标注的方法，落库操作日志
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LogAspect {

    private final OperationLogMapper operationLogMapper;

    @Around("@annotation(logOperation)")
    public Object around(ProceedingJoinPoint joinPoint, LogOperation logOperation) throws Throwable {
        long start = System.currentTimeMillis();
        OperationLog operationLog = new OperationLog();
        operationLog.setModule(logOperation.module());
        operationLog.setAction(logOperation.value());
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        operationLog.setMethod(signature.getDeclaringType().getSimpleName() + "." + signature.getName());
        // 参数摘要（脱敏：屏蔽密码字段）
        String params = Arrays.stream(joinPoint.getArgs())
                .map(arg -> {
                    String s = String.valueOf(arg);
                    return s.replaceAll("(?i)(password[=:]\\s*)[^,\\s)]+", "$1***");
                })
                .collect(Collectors.joining("; "));
        if (params.length() > 1000) {
            params = params.substring(0, 1000);
        }
        operationLog.setParams(params);
        // 请求信息
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            operationLog.setMethod(request.getMethod() + " " + request.getRequestURI());
            operationLog.setIp(getIp(request));
        }
        // 操作人（登录接口在拦截器白名单内，proceed 前后 UserContext 均为空，需从参数回填）
        fillOperator(operationLog, joinPoint);
        try {
            Object result = joinPoint.proceed();
            operationLog.setStatus(1);
            if (operationLog.getUserId() == null) {
                fillOperator(operationLog, joinPoint);
            }
            return result;
        } catch (Throwable e) {
            operationLog.setStatus(0);
            operationLog.setErrorMsg(e.getMessage() != null && e.getMessage().length() > 500
                    ? e.getMessage().substring(0, 500) : e.getMessage());
            if (operationLog.getUserId() == null) {
                fillOperator(operationLog, joinPoint);
            }
            throw e;
        } finally {
            operationLog.setCostMs(System.currentTimeMillis() - start);
            try {
                operationLogMapper.insert(operationLog);
            } catch (Exception ex) {
                log.warn("操作日志写入失败：{}", ex.getMessage());
            }
        }
    }

    /** 优先取登录上下文；白名单接口（如登录）从请求参数的 username 字段回填 */
    private void fillOperator(OperationLog operationLog, ProceedingJoinPoint joinPoint) {
        LoginUser user = UserContext.get();
        if (user != null) {
            operationLog.setUserId(user.getUserId());
            operationLog.setUsername(user.getUsername());
            return;
        }
        for (Object arg : joinPoint.getArgs()) {
            if (arg == null) {
                continue;
            }
            try {
                java.lang.reflect.Method getter = arg.getClass().getMethod("getUsername");
                Object username = getter.invoke(arg);
                if (username instanceof String s && !s.isBlank()) {
                    operationLog.setUsername(s);
                    return;
                }
            } catch (ReflectiveOperationException ignored) {
                // 参数对象没有 username 字段，跳过
            }
        }
    }

    private String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
