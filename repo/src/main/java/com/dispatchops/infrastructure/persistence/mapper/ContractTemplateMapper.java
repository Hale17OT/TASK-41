package com.dispatchops.infrastructure.persistence.mapper;

import com.dispatchops.domain.model.ContractTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ContractTemplateMapper {

    ContractTemplate findById(@Param("id") Long id);

    List<ContractTemplate> findAll(@Param("offset") int offset, @Param("limit") int limit);

    int insert(ContractTemplate tpl);

    int update(ContractTemplate tpl);
    int countAll();
}
