package com.dispatchops.infrastructure.persistence.mapper;

import com.dispatchops.domain.model.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface NotificationMapper {

    Notification findById(@Param("id") Long id);

    List<Notification> findByRecipientId(@Param("recipientId") Long recipientId,
                                         @Param("offset") int offset,
                                         @Param("limit") int limit);

    List<Notification> findUnreadByRecipientId(@Param("recipientId") Long recipientId);

    List<Notification> findNewSince(@Param("recipientId") Long recipientId,
                                    @Param("since") LocalDateTime since);

    int countUnread(@Param("recipientId") Long recipientId);

    List<Notification> findReadByRecipientId(@Param("recipientId") Long recipientId,
                                              @Param("offset") int offset,
                                              @Param("limit") int limit);

    int countReadByRecipientId(@Param("recipientId") Long recipientId);

    List<Notification> findUnreadByRecipientIdPaged(@Param("recipientId") Long recipientId,
                                                     @Param("offset") int offset,
                                                     @Param("limit") int limit);

    int insert(Notification n);

    int markRead(@Param("id") Long id);

    int markAllRead(@Param("recipientId") Long recipientId);
    int countAll();
    int countByRecipientId(@org.apache.ibatis.annotations.Param("recipientId") Long recipientId);
}
