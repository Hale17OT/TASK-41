package com.dispatchops.infrastructure.persistence.mapper;

import com.dispatchops.domain.model.TaskRecipient;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TaskRecipientMapper {

    List<TaskRecipient> findByTaskId(@Param("taskId") Long taskId);

    List<TaskRecipient> findByUserAndInboxType(@Param("userId") Long userId,
                                               @Param("inboxType") String inboxType,
                                               @Param("offset") int offset,
                                               @Param("limit") int limit);

    int countByUserAndInboxType(@Param("userId") Long userId,
                                @Param("inboxType") String inboxType);

    int insert(TaskRecipient recipient);

    int updateInboxType(@Param("taskId") Long taskId,
                        @Param("userId") Long userId,
                        @Param("oldType") String oldType,
                        @Param("newType") String newType);

    int markRead(@Param("id") Long id);
}
