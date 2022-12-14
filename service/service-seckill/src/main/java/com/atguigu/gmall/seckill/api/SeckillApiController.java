package com.atguigu.gmall.seckill.api;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.activity.SeckillGoods;
import com.atguigu.gmall.model.vo.seckill.SeckillOrderConfirmVo;
import com.atguigu.gmall.seckill.biz.SeckillBizService;
import com.atguigu.gmall.seckill.service.SeckillGoodsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping("/api/inner/rpc/seckill")
@RestController
public class SeckillApiController {
    @Autowired
    SeckillGoodsService seckillGoodsService;

    @Autowired
    SeckillBizService seckillBizService;

    /**
     * 获取当前参与秒杀的所有商品
     * @return
     */
    @GetMapping("/current/goods/list")
    public Result<List<SeckillGoods>> getCurrentDaySeckillGoodsList(){
//        List<SeckillGoods> goodsList = seckillGoodsService
//                .getCurrentDaySeckillGoodsList();
        List<SeckillGoods> goodsList = seckillGoodsService.getCurrentDaySeckillGoodsCache();
        return Result.ok(goodsList);
    }

    /**
     *  获取商品详情
     * @param skuId
     * @return
     */
    @GetMapping("/good/detail/{skuId}")
    public Result<SeckillGoods> getSeckillGoods(@PathVariable("skuId") Long skuId){
        SeckillGoods goods = seckillGoodsService.getGoodDetail(skuId);
        return Result.ok(goods);
    }

    /**
     * 获取秒杀确认页数据
     * @param skuId
     * @return
     */
    @GetMapping("/order/confirmvo/{skuId}")
    public Result<SeckillOrderConfirmVo> getSeckillOrderConfirmVo(@PathVariable("skuId") Long skuId){
        SeckillOrderConfirmVo vo = seckillBizService.getSeckillOrderConfirmVo(skuId);
        return Result.ok(vo);
    }
}
