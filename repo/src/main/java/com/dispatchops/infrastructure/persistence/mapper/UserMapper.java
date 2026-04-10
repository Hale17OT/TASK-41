package com.dispatchops.infrastructure.persistence.mapper;

import com.dispatchops.domain.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserMapper {

    User findById(@Param("id") Long id);

    User findByUsername(@Param("username") String username);

    List<User> findByRole(@Param("role") String role);

    List<User> findAll(@Param("offset") int offset, @Param("limit") int limit);

    List<User> findAllFiltered(@Param("role") String role, @Param("offset") int offset, @Param("limit") int limit);

    int countAll();

    int countByRole(@Param("role") String role);

    int updatePassword(@Param("id") Long id, @Param("passwordHash") String passwordHash);

    int insert(User user);

    int update(User user);

    int updateFailedAttempts(@Param("id") Long id,
                             @Param("failedAttempts") int failedAttempts,
                             @Param("lockoutExpiry") LocalDateTime lockoutExpiry);

    int resetFailedAttempts(@Param("id") Long id);

    int deactivate(@Param("id") Long id);
}
