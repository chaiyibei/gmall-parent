package com.atguigu.gmall.web.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.feign.order.OrderFeignClient;
import com.atguigu.gmall.model.vo.order.OrderConfirmDataVo;
import com.sun.org.apache.bcel.internal.generic.RETURN;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class OrderTradeController {
    @Autowired
    OrderFeignClient orderFeignClient;

    /**
     * 跳转到订单确认页
     * @return
     *
     * detailArrayList  totalNum  totalAmount  userAddressList  tradeNo
     */
    @GetMapping("/trade.html")
    public String tradeHtml(Model model){
        Result<OrderConfirmDataVo> orderConfirmData = orderFeignClient.getOrderConfirmData();
        if (orderConfirmData.isOk()){
            OrderConfirmDataVo data = orderConfirmData.getData();

            model.addAttribute("detailArrayList",data.getDetailArrayList());
            model.addAttribute("totalNum",data.getTotalNum());
            model.addAttribute("totalAmount",data.getTotalAmount());
            //用户收货地址列表
            model.addAttribute("userAddressList",data.getUserAddressList());
            //追踪订单的 交易号
            model.addAttribute("tradeNo",data.getTradeNo());
        }

        return "order/trade";
    }

    /**
     * 订单列表页
     * @return
     */
    @GetMapping("/myOrder.html")
    public String myOrderHtml(){
        return "order/myOrder";
    }

}
