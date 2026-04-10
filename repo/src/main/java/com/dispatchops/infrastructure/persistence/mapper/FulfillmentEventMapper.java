package com.dispatchops.infrastructure.persistence.mapper;

import com.dispatchops.domain.model.FulfillmentEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FulfillmentEventMapper {

    FulfillmentEvent findById(@Param("id") Long id);

    List<FulfillmentEvent> findByJobId(@Param("jobId") Long jobId);

    int insert(FulfillmentEvent event);
}
