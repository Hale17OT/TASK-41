package com.dispatchops.infrastructure.persistence.mapper;

import com.dispatchops.domain.model.LedgerEntry;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface LedgerEntryMapper {

    LedgerEntry findById(@Param("id") Long id);

    List<LedgerEntry> findByAccountId(@Param("accountId") Long accountId,
                                      @Param("offset") int offset,
                                      @Param("limit") int limit);

    List<LedgerEntry> findByPaymentId(@Param("paymentId") Long paymentId);

    BigDecimal calculateBalance(@Param("accountId") Long accountId);

    List<LedgerEntry> findByDateRange(@Param("from") LocalDate from,
                                      @Param("to") LocalDate to);

    int insert(LedgerEntry entry);
    int countByAccountId(@org.apache.ibatis.annotations.Param("accountId") Long accountId);
}
