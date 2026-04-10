package com.dispatchops.infrastructure.persistence.mapper;

import com.dispatchops.domain.model.CreditLevelSnapshot;
import org.apache.ibatis.annotations.*;

public interface CreditLevelSnapshotMapper {

    @Insert("INSERT INTO credit_level_snapshots (courier_id, level, max_concurrent, avg_rating_30d, violations_active, calculated_at) " +
            "VALUES (#{courierId}, #{level}, #{maxConcurrent}, #{avgRating30d}, #{violationsActive}, #{calculatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(CreditLevelSnapshot snapshot);

    @Select("SELECT * FROM credit_level_snapshots WHERE courier_id = #{courierId} ORDER BY calculated_at DESC LIMIT 1")
    CreditLevelSnapshot findLatestByCourierId(@Param("courierId") Long courierId);
}
