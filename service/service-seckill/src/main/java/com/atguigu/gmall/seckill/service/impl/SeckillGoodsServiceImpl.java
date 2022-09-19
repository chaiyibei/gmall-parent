package com.atguigu.gmall.seckill.service.impl;

import com.atguigu.gmall.common.util.DateUtil;
import com.atguigu.gmall.model.activity.SeckillGoods;
import com.atguigu.gmall.seckill.service.SeckillGoodsCacheOpsService;
import com.atguigu.gmall.seckill.service.SeckillGoodsService;
import com.atguigu.gmall.seckill.mapper.SeckillGoodsMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
* @author 柴小贝
* @description 针对表【seckill_goods】的数据库操作Service实现
* @createDate 2022-09-19 19:50:00
*/
@Service
public class SeckillGoodsServiceImpl extends ServiceImpl<SeckillGoodsMapper, SeckillGoods>
    implements SeckillGoodsService{
    @Autowired
    SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    SeckillGoodsCacheOpsService cacheOpsService;

    @Override
    public List<SeckillGoods> getCurrentDaySeckillGoodsList() {
        String date = DateUtil.formatDate(new Date());

        List<SeckillGoods> goodsList = seckillGoodsMapper.getCurrentDaySeckillGoodsList(date);

        return goodsList;
    }

    @Override
    public List<SeckillGoods> getCurrentDaySeckillGoodsCache() {

        return cacheOpsService.getSeckillGoodsFromLocal();
    }

    @Override
    public SeckillGoods getGoodDetail(Long skuId) {
        SeckillGoods goods = cacheOpsService.getSeckillGoodsDetail(skuId);
        return goods;
    }
}




