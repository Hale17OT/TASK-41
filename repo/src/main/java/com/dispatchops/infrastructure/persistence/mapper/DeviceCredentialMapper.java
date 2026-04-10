package com.dispatchops.infrastructure.persistence.mapper;

import com.dispatchops.domain.model.DeviceCredential;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DeviceCredentialMapper {

    DeviceCredential findByDeviceId(@Param("deviceId") String deviceId);

    int insert(DeviceCredential dc);
}
