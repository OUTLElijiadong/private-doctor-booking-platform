package com.doctor.appointment.security;

import com.doctor.appointment.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * 登录认证 + 角色鉴权拦截器
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        // 1. 解析 Token
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            writeError(response, 401, "未登录或登录已过期");
            return false;
        }
        try {
            Claims claims = jwtUtils.parse(header.substring(7));
            LoginUser loginUser = new LoginUser();
            loginUser.setUserId(Long.valueOf(claims.getSubject()));
            loginUser.setUsername(claims.get("username", String.class));
            loginUser.setRole(claims.get("role", String.class));
            UserContext.set(loginUser);
        } catch (Exception e) {
            writeError(response, 401, "登录状态无效，请重新登录");
            return false;
        }
        // 2. 角色校验（方法注解优先于类注解）
        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (requireRole == null) {
            requireRole = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        }
        if (requireRole != null) {
            String role = UserContext.getRole();
            boolean allowed = Arrays.asList(requireRole.value()).contains(role);
            if (!allowed) {
                writeError(response, 403, "无权限执行该操作");
                return false;
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private void writeError(HttpServletResponse response, int code, String message) throws Exception {
        response.setStatus(200);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(Result.error(code, message)));
    }
}
