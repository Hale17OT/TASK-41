package com.dispatchops.infrastructure.persistence.mapper;

import com.dispatchops.domain.model.ContractTemplateVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ContractTemplateVersionMapper {

    ContractTemplateVersion findById(@Param("id") Long id);

    List<ContractTemplateVersion> findByTemplateId(@Param("templateId") Long templateId);

    ContractTemplateVersion findLatestVersion(@Param("templateId") Long templateId);

    int insert(ContractTemplateVersion v);
}
