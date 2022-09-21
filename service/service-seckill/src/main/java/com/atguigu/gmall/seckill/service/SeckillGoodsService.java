package com.atguigu.gmall.seckill.service;

import com.atguigu.gmall.model.activity.SeckillGoods;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author 柴小贝
* @description 针对表【seckill_goods】的数据库操作Service
* @createDate 2022-09-19 19:50:00
*/
public interface SeckillGoodsService extends IService<SeckillGoods> {
    /**
     * 获取当前参与秒杀的所有商品
     * @return
     */
    List<SeckillGoods> getCurrentDaySeckillGoodsList();

    /**
     * 从缓存中获取当天秒杀商品
     * @return
     */
    List<SeckillGoods> getCurrentDaySeckillGoodsCache();

    /**
     * 秒杀商品详情
     * @param skuId
     * @return
     */
    SeckillGoods getGoodDetail(Long skuId);

    /**
     * 扣减库存
     * @param skuId
     */
    void deductSeckillGoods(Long skuId);
}
