package com.dispatchops.infrastructure.persistence.mapper;

import com.dispatchops.domain.model.ShippingTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ShippingTemplateMapper {

    ShippingTemplate findById(@Param("id") Long id);

    List<ShippingTemplate> findAll(@Param("offset") int offset, @Param("limit") int limit);

    int insert(ShippingTemplate t);

    int update(ShippingTemplate t);
    int countAll();
}
