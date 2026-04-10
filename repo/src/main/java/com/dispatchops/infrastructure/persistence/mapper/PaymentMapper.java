package com.dispatchops.infrastructure.persistence.mapper;

import com.dispatchops.domain.model.Payment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PaymentMapper {

    Payment findById(@Param("id") Long id);

    Payment findByIdempotencyKey(@Param("key") String key);

    List<Payment> findByJobId(@Param("jobId") Long jobId);

    List<Payment> findByStatus(@Param("status") String status,
                               @Param("offset") int offset,
                               @Param("limit") int limit);

    int countByStatus(@Param("status") String status);

    List<Payment> findFiltered(@Param("status") String status,
                                @Param("from") String from,
                                @Param("to") String to,
                                @Param("offset") int offset,
                                @Param("limit") int limit);

    int countFiltered(@Param("status") String status,
                       @Param("from") String from,
                       @Param("to") String to);

    int insert(Payment p);

    int updateStatus(@Param("id") Long id,
                     @Param("newStatus") String newStatus,
                     @Param("expectedStatus") String expectedStatus);

    int settle(@Param("id") Long id,
               @Param("settledBy") Long settledBy,
               @Param("settledAt") LocalDateTime settledAt,
               @Param("refundEligibleUntil") LocalDateTime refundEligibleUntil);
}
