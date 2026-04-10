package com.dispatchops.infrastructure.persistence.mapper;

import com.dispatchops.domain.model.CallbackEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CallbackEventMapper {
    CallbackEvent findByEventId(@Param("eventId") String eventId);
    List<CallbackEvent> findByDeviceId(@Param("deviceId") String deviceId, @Param("offset") int offset, @Param("limit") int limit);
    List<CallbackEvent> findByStatus(@Param("status") String status, @Param("offset") int offset, @Param("limit") int limit);
    int insert(CallbackEvent event);
    int updateStatus(@Param("id") Long id, @Param("status") String status,
                     @Param("verifiedAt") java.time.LocalDateTime verifiedAt,
                     @Param("processedAt") java.time.LocalDateTime processedAt,
                     @Param("failureReason") String failureReason);
}
