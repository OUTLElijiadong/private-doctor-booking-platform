package com.doctor.appointment.config;

import com.doctor.appointment.security.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置：跨域 + 登录拦截器
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        // 认证白名单
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/auth/password/reset",
                        // 登录页公开平台统计（仅聚合数字）
                        "/api/public/**",
                        // 接口文档
                        "/api/v3/api-docs/**",
                        "/api/doc.html",
                        "/api/webjars/**",
                        "/api/swagger-resources/**",
                        "/api/favicon.ico",
                        "/error"
                );
    }
}
