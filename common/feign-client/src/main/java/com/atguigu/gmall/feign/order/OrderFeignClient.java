package com.atguigu.gmall.feign.order;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.vo.order.OrderConfirmDataVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/inner/rpc/order")
@FeignClient("service-order")
public interface OrderFeignClient {
    /**
     * 获取订单确认页需要的数据
     * @return
     */
    @GetMapping("/confirm/data")
    Result<OrderConfirmDataVo> getOrderConfirmData();

    /**
     * 根据订单id获得订单信息
     * @param orderId
     * @return
     */
    @GetMapping("/info/{orderId}")
    Result<OrderInfo> getOrderInfo(@PathVariable("orderId") Long orderId);
}
