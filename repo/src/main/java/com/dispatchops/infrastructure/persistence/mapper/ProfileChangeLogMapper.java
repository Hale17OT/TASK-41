package com.dispatchops.infrastructure.persistence.mapper;

import com.dispatchops.domain.model.ProfileChangeLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProfileChangeLogMapper {

    List<ProfileChangeLog> findByProfileId(@Param("profileId") Long profileId,
                                           @Param("offset") int offset,
                                           @Param("limit") int limit);

    int insert(ProfileChangeLog log);
}
