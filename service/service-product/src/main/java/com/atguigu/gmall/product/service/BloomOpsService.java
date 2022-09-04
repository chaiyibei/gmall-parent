package com.atguigu.gmall.product.service;

public interface BloomOpsService {
    /**
     * 重建指定布隆过滤器
     * @param bloomName
     */
    void rebuildBloom(String bloomName);
}
