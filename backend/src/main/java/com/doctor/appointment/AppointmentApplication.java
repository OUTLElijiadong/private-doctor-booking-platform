package com.doctor.appointment;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 私人医生预约服务平台启动类
 */
@SpringBootApplication
@MapperScan("com.doctor.appointment.**.mapper")
public class AppointmentApplication {

    public static void main(String[] args) {
        SpringApplication.run(AppointmentApplication.class, args);
    }
}
