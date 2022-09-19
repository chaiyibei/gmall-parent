package com.atguigu.gmall.seckill.service;

import com.atguigu.gmall.model.activity.SeckillGoods;

import java.util.List;

public interface SeckillGoodsCacheOpsService {
    void upSeckillGoods(List<SeckillGoods> list);

    void clearCache();

    List<SeckillGoods> getSeckillGoodsFromLocal();

    List<SeckillGoods> getSeckillGoodsFromRemote();

    /**
     * 本地与redis同步缓存
     */
    void syncRedisCache();

    SeckillGoods getSeckillGoodsDetail(Long skuId);
}
