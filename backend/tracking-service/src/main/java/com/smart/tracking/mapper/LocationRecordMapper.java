package com.smart.tracking.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smart.tracking.entity.LocationRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface LocationRecordMapper extends BaseMapper<LocationRecord> {

    /** 查询每条路线最新位置（PostgreSQL DISTINCT ON） */
    @Select("SELECT DISTINCT ON (route_id) id, route_id, driver_id, lng, lat, speed, heading, recorded_at, created_at " +
            "FROM location_records ORDER BY route_id, recorded_at DESC")
    List<LocationRecord> selectLiveLocations();
}
