package com.wuba.wlock.registry.admin.domain.request;

import lombok.Data;

@Data
public class KeyUpdateReq {
    private String id;
    private Integer autoRenew;
    private Integer qps;
    private String description;
}
