package com.atguigu.gmall.seckill.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.seckill.biz.SeckillBizService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/activity/seckill/auth")
@RestController
public class SeckillRestController {
    @Autowired
    SeckillBizService seckillBizService;

    /**
     * 生成秒杀码，隐藏真正的秒杀地址，开始秒杀排队
     * @param skuId
     * @return
     */
    @GetMapping("/getSeckillSkuIdStr/{skuId}")
    public Result getSeckillCode(@PathVariable("skuId")Long skuId){
        String skuIdStr = seckillBizService.generateSeckillCode(skuId);
        return Result.ok(skuIdStr);
    }

    /**
     * 秒杀预下单
     */
    @PostMapping("/seckillOrder/{skuId}")
    public Result seckillOrder(@PathVariable("skuId") Long skuId,
                               @RequestParam("skuIdStr") String skuIdStr){
        ResultCodeEnum codeEnum = seckillBizService.seckillOrder(skuId,skuIdStr);
        //1、秒杀码是否合法
        //2、走整个秒杀流程
        //3、告诉页面结果
        return Result.build("",codeEnum);
    }

    /**
     * 检查秒杀订单的状态
     * @param skuId
     * @return
     */
    @GetMapping("/checkOrder/{skuId}")
    public Result checkOrder(@PathVariable("skuId") Long skuId){
        ResultCodeEnum codeEnum = seckillBizService.checkSeckillOrderStatus(skuId);
        return Result.build("",codeEnum);
    }

    //http://api.gmall.com/api/activity/seckill/auth/submitOrder
    @PostMapping("/submitOrder")
    public Result submitOrder(@RequestBody OrderInfo orderInfo){
        //
        Long orderId = seckillBizService.submitSeckillOrder(orderInfo);
        //响应订单id
        return Result.ok(orderId.toString());
    }
}
