package com.atguigu.gmall.feign.seckill;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.activity.SeckillGoods;
import com.atguigu.gmall.model.vo.seckill.SeckillOrderConfirmVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@RequestMapping("/api/inner/rpc/seckill")
@FeignClient("service-seckill")
public interface SeckillFeignClient {
    /**
     * 获取当前参与秒杀的所有商品
     * @return
     */
    @GetMapping("/current/goods/list")
    Result<List<SeckillGoods>> getCurrentDaySeckillGoodsList();

    /**
     *  获取商品详情
     * @param skuId
     * @return
     */
    @GetMapping("/good/detail/{skuId}")
    Result<SeckillGoods> getSeckillGoods(@PathVariable("skuId") Long skuId);

    /**
     * 获取秒杀确认页数据
     * @param skuId
     * @return
     */
    @GetMapping("/order/confirmvo/{skuId}")
    Result<SeckillOrderConfirmVo> getSeckillOrderConfirmVo(@PathVariable("skuId") Long skuId);
}
