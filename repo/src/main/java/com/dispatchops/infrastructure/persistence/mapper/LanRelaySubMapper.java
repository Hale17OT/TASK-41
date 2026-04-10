package com.dispatchops.infrastructure.persistence.mapper;

import com.dispatchops.domain.model.LanRelaySub;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LanRelaySubMapper {

    List<LanRelaySub> findByUserId(@Param("userId") Long userId);

    List<LanRelaySub> findByTopic(@Param("topic") String topic);

    int insert(LanRelaySub sub);

    int deactivate(@Param("userId") Long userId, @Param("topic") String topic);
}
