package com.doctor.appointment.modules.schedule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.doctor.appointment.modules.schedule.entity.Schedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ScheduleMapper extends BaseMapper<Schedule> {

    /** 预约成功后号源 +1（乐观控制，号满返回 0） */
    @Update("UPDATE schedule SET booked_count = booked_count + 1 " +
            "WHERE id = #{id} AND status = 1 AND booked_count < max_count")
    int incrBooked(@Param("id") Long id);

    /** 取消预约后号源 -1 */
    @Update("UPDATE schedule SET booked_count = booked_count - 1 " +
            "WHERE id = #{id} AND booked_count > 0")
    int decrBooked(@Param("id") Long id);
}
