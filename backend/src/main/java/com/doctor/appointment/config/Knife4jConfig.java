package com.doctor.appointment.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / OpenAPI 接口文档配置
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("私人医生预约服务平台接口文档")
                        .description("基于 Spring Boot 3 + Vue 3 的私人医生预约服务平台 RESTful API")
                        .version("1.0.0")
                        .contact(new Contact().name("张甜甜")));
    }
}
