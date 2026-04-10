package com.dispatchops.infrastructure.persistence.mapper;

import com.dispatchops.domain.model.DeliveryJob;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface DeliveryJobMapper {

    DeliveryJob findById(@Param("id") Long id);

    DeliveryJob findByTrackingNumber(@Param("trackingNumber") String trackingNumber);

    List<DeliveryJob> findByStatus(@Param("status") String status,
                                   @Param("offset") int offset,
                                   @Param("limit") int limit);

    List<DeliveryJob> findByCourierId(@Param("courierId") Long courierId,
                                      @Param("offset") int offset,
                                      @Param("limit") int limit);

    List<DeliveryJob> findByCourierIdAndStatus(@Param("courierId") Long courierId,
                                                @Param("status") String status,
                                                @Param("offset") int offset,
                                                @Param("limit") int limit);

    int countByCourierId(@Param("courierId") Long courierId,
                          @Param("status") String status);

    List<DeliveryJob> findIdleLongerThan(@Param("minutes") int minutes);

    int countByCourierAndActiveStatuses(@Param("courierId") Long courierId);

    List<DeliveryJob> search(@Param("keyword") String keyword,
                             @Param("status") String status,
                             @Param("startDate") LocalDate startDate,
                             @Param("endDate") LocalDate endDate,
                             @Param("offset") int offset,
                             @Param("limit") int limit);

    int countAll();

    int countByStatus(@Param("status") String status);

    int insert(DeliveryJob job);

    int updateStatus(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("expectedVersion") int expectedVersion);

    int updateCourier(@Param("id") Long id,
                      @Param("courierId") Long courierId);

    int updateLastEventAt(@Param("id") Long id,
                          @Param("lastEventAt") LocalDateTime lastEventAt);

    int updateCustomerToken(@Param("id") Long id,
                            @Param("customerToken") String customerToken);

    int adminOverride(@Param("id") Long id,
                      @Param("overrideComment") String overrideComment);
}
