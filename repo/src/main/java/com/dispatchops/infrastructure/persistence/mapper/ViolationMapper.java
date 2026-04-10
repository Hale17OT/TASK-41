package com.dispatchops.infrastructure.persistence.mapper;

import com.dispatchops.domain.model.Violation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ViolationMapper {

    Violation findById(@Param("id") Long id);

    List<Violation> findByCourierId(@Param("courierId") Long courierId);

    int countActiveByCourierId(@Param("courierId") Long courierId);

    int insert(Violation v);

    int deactivate(@Param("id") Long id);
}
