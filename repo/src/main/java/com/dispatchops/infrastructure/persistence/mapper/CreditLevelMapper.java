package com.dispatchops.infrastructure.persistence.mapper;

import com.dispatchops.domain.model.CreditLevelSnapshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CreditLevelMapper {

    CreditLevelSnapshot findByCourierId(@Param("courierId") Long courierId);

    int upsert(CreditLevelSnapshot snapshot);
}
