package com.atguigu.gmall.order.api;

import com.atguigu.gmall.common.auth.AuthUtils;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.vo.order.OrderConfirmDataVo;
import com.atguigu.gmall.model.vo.user.UserAuthInfo;
import com.atguigu.gmall.order.biz.OrderBizService;
import com.atguigu.gmall.order.service.OrderInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/inner/rpc/order")
@RestController
public class OrderApiController {
    @Autowired
    OrderBizService orderBizService;

    @Autowired
    OrderInfoService orderInfoService;

    /**
     * 获取订单确认页需要的数据
     * @return
     */
    @GetMapping("/confirm/data")
    public Result<OrderConfirmDataVo> getOrderConfirmData(){
        OrderConfirmDataVo confirmDataVo = orderBizService.getOrderConfirmData();
        return Result.ok(confirmDataVo);
    }

    /**
     * 根据订单id获得订单信息
     * @param orderId
     * @return
     */
    @GetMapping("/info/{orderId}")
    public Result<OrderInfo> getOrderInfo(@PathVariable("orderId") Long orderId){
        Long userId = AuthUtils.getCurrentAuthInfo().getUserId();
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getId, orderId)
                .eq(OrderInfo::getUserId, userId);
        OrderInfo orderInfo = orderInfoService.getOne(wrapper);
        return Result.ok(orderInfo);
    }

    /**
     * 保存秒杀单
     * @param orderInfo
     * @return
     */
    @PostMapping("/seckillorder/submit")
    public Result<Long> submitSeckillOrder(@RequestBody OrderInfo orderInfo){
        Long orderId = orderInfoService.submitSeckillOrder(orderInfo);
        return Result.ok(orderId);
    }
}
