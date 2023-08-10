package com.wuba.wlock.starter.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;


@Data
@ConfigurationProperties("wlock")
public class WLockProperties {

    private Boolean enabled = true;

    private String key;

    private String registryServerIp;

    private Integer registryServerPort;

    private Long expireTime;

    private Integer maxWaitTime;

    private int timeoutForReq;

    private int retryTimes;
}
