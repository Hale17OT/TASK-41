package com.dispatchops.infrastructure.persistence.mapper;

import com.dispatchops.domain.model.ReconciliationItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ReconciliationItemMapper {

    ReconciliationItem findById(@Param("id") Long id);

    List<ReconciliationItem> findByStatus(@Param("status") String status,
                                          @Param("offset") int offset,
                                          @Param("limit") int limit);

    int insert(ReconciliationItem item);

    int resolve(@Param("id") Long id,
                @Param("status") String status,
                @Param("resolvedBy") Long resolvedBy,
                @Param("note") String note,
                @Param("resolvedAt") LocalDateTime resolvedAt);
    int countByStatus(@org.apache.ibatis.annotations.Param("status") String status);
}
