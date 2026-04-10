package com.dispatchops.infrastructure.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ProfileFieldVisibilityMapper {

    List<Map<String, Object>> findByUserId(@Param("userId") Long userId);

    int upsert(@Param("userId") Long userId, @Param("fieldName") String fieldName, @Param("visibilityTier") int visibilityTier);
}
