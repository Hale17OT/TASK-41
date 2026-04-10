package com.dispatchops.infrastructure.persistence.mapper;

import com.dispatchops.domain.model.UserProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserProfileMapper {

    UserProfile findByUserId(@Param("userId") Long userId);

    int insert(UserProfile p);

    int update(UserProfile p);
}
