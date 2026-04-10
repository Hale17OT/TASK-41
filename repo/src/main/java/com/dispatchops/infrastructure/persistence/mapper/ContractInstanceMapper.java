package com.dispatchops.infrastructure.persistence.mapper;

import com.dispatchops.domain.model.ContractInstance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ContractInstanceMapper {

    ContractInstance findById(@Param("id") Long id);

    List<ContractInstance> findByJobId(@Param("jobId") Long jobId);

    List<ContractInstance> findByStatus(@Param("status") String status,
                                       @Param("offset") int offset,
                                       @Param("limit") int limit);

    int insert(ContractInstance inst);

    int updateStatus(@Param("id") Long id,
                     @Param("status") String status);
}
