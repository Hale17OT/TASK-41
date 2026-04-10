package com.dispatchops.infrastructure.persistence.mapper;

import com.dispatchops.domain.model.MediaAttachment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MediaAttachmentMapper {

    MediaAttachment findById(@Param("id") Long id);

    List<MediaAttachment> findByEntity(@Param("entityType") String entityType,
                                       @Param("entityId") Long entityId,
                                       @Param("maxVisibility") int maxVisibility);

    int insert(MediaAttachment a);

    int deactivate(@Param("id") Long id);
}
