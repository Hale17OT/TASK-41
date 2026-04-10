package com.dispatchops.infrastructure.persistence.mapper;

import com.dispatchops.domain.model.TaskComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TaskCommentMapper {

    List<TaskComment> findByTaskId(@Param("taskId") Long taskId);

    int insert(TaskComment comment);
}
