package com.dispatchops.infrastructure.persistence.mapper;

import com.dispatchops.domain.model.TrendingTerm;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TrendingTermMapper {

    List<TrendingTerm> findRecent(@Param("limit") int limit);

    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);

    int insert(TrendingTerm t);
}
