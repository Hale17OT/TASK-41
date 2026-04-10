package com.dispatchops.infrastructure.persistence.mapper;

import com.dispatchops.domain.model.CredibilityRating;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface CredibilityRatingMapper {

    CredibilityRating findById(@Param("id") Long id);

    List<CredibilityRating> findByCourierId(@Param("courierId") Long courierId,
                                            @Param("offset") int offset,
                                            @Param("limit") int limit);

    BigDecimal calculateAvgRating30d(@Param("courierId") Long courierId);

    int insert(CredibilityRating rating);

    List<CredibilityRating> findByJobId(@Param("jobId") Long jobId);

    int excludeRating(@Param("id") Long id);
    int countByCourierId(@org.apache.ibatis.annotations.Param("courierId") Long courierId);
}
