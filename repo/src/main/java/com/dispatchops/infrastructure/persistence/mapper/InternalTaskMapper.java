package com.dispatchops.infrastructure.persistence.mapper;

import com.dispatchops.domain.model.InternalTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface InternalTaskMapper {

    InternalTask findById(@Param("id") Long id);

    List<InternalTask> findAll(@Param("offset") int offset, @Param("limit") int limit);

    List<InternalTask> findByJobId(@Param("jobId") Long jobId);

    int countAll();

    int insert(InternalTask task);

    int updateStatus(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("expectedVersion") int expectedVersion);

    int updateDueTime(@Param("id") Long id,
                      @Param("dueTime") LocalDateTime dueTime);

    List<InternalTask> findCalendarTasks(@Param("userId") Long userId,
                                          @Param("from") LocalDateTime from,
                                          @Param("to") LocalDateTime to);
}
