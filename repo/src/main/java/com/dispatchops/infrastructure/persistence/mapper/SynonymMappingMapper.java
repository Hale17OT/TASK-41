package com.dispatchops.infrastructure.persistence.mapper;

import com.dispatchops.domain.model.SynonymMapping;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SynonymMappingMapper {

    List<SynonymMapping> findByTerm(@Param("term") String term);

    int insert(SynonymMapping m);
}
