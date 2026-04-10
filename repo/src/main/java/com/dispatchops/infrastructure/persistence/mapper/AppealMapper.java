package com.dispatchops.infrastructure.persistence.mapper;

import com.dispatchops.domain.model.Appeal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AppealMapper {

    Appeal findById(@Param("id") Long id);

    List<Appeal> findByStatus(@Param("status") String status,
                              @Param("offset") int offset,
                              @Param("limit") int limit);

    List<Appeal> findByCourierId(@Param("courierId") Long courierId);

    int insert(Appeal a);

    int resolve(@Param("id") Long id,
                @Param("status") String status,
                @Param("reviewerId") Long reviewerId,
                @Param("reviewerComment") String reviewerComment,
                @Param("resolvedAt") LocalDateTime resolvedAt);
    int countByCourierId(@org.apache.ibatis.annotations.Param("courierId") Long courierId);
    int countByStatus(@org.apache.ibatis.annotations.Param("status") String status);
}
