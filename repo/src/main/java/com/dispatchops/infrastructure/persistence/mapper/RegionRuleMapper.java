package com.dispatchops.infrastructure.persistence.mapper;

import com.dispatchops.domain.model.RegionRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface RegionRuleMapper {

    RegionRule findById(@Param("id") Long id);

    List<RegionRule> findByTemplateId(@Param("templateId") Long templateId);

    List<RegionRule> findMatchingRules(@Param("state") String state,
                                      @Param("zip") String zip,
                                      @Param("weight") BigDecimal weight,
                                      @Param("amount") BigDecimal amount);

    int insert(RegionRule r);

    int update(RegionRule r);

    int delete(@Param("id") Long id);
}
