package com.dispatchops.infrastructure.persistence.mapper;

import com.dispatchops.domain.model.SigningRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SigningRecordMapper {

    SigningRecord findById(@Param("id") Long id);

    List<SigningRecord> findByContractInstanceId(@Param("contractInstanceId") Long contractInstanceId);

    int countByContractInstanceId(@Param("contractInstanceId") Long contractInstanceId);

    int insert(SigningRecord sr);
}
