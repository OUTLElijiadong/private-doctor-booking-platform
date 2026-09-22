package com.doctor.appointment.modules.appointment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.doctor.appointment.modules.appointment.entity.Appointment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface AppointmentMapper extends BaseMapper<Appointment> {

    /** 预约趋势：近 N 天每日预约量 */
    @Select("SELECT DATE(created_at) AS label, COUNT(*) AS value FROM appointment " +
            "WHERE created_at >= #{start} GROUP BY DATE(created_at) ORDER BY label")
    List<Map<String, Object>> countTrend(@Param("start") LocalDate start);

    /** 各科室预约量分布（LEFT JOIN，未分配/已删科室归入"未分配"） */
    @Select("SELECT COALESCE(d.name, '未分配') AS label, COUNT(*) AS value FROM appointment a " +
            "LEFT JOIN department d ON a.department_id = d.id GROUP BY d.name ORDER BY value DESC")
    List<Map<String, Object>> countByDepartment();

    /** 医生接诊量 TOP10 */
    @Select("SELECT u.real_name AS label, COUNT(*) AS value FROM appointment a " +
            "JOIN sys_user u ON a.doctor_id = u.id WHERE a.status = 2 " +
            "GROUP BY u.real_name ORDER BY value DESC LIMIT 10")
    List<Map<String, Object>> doctorWorkload();

    /** 指定医生近 N 天接诊趋势（按就诊日期统计，限定今天及以前） */
    @Select("SELECT appoint_date AS label, COUNT(*) AS value FROM appointment " +
            "WHERE doctor_id = #{doctorId} AND appoint_date >= #{start} AND appoint_date <= CURRENT_DATE AND status != 3 " +
            "GROUP BY appoint_date ORDER BY label")
    List<Map<String, Object>> doctorTrend(@Param("doctorId") Long doctorId, @Param("start") LocalDate start);
}
