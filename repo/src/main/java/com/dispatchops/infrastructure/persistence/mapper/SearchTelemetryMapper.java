package com.dispatchops.infrastructure.persistence.mapper;

import com.dispatchops.domain.model.SearchTelemetry;
import com.dispatchops.domain.model.TrendingTerm;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SearchTelemetryMapper {

    int insert(SearchTelemetry t);

    List<TrendingTerm> aggregateTopTerms(@Param("since") LocalDateTime since,
                                         @Param("limit") int limit);
}
