package com.atguigu.gmall.pay.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayConfig;
import com.alipay.api.internal.util.AlipaySignature;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.Jsons;
import com.atguigu.gmall.pay.service.AlipayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.spring.web.json.Json;

import java.util.Map;
@Slf4j
@RequestMapping("/api/payment")
@Controller
public class PayController {
    @Autowired
    AlipayService alipayService;

    //  /api/payment/alipay/submit/777611808625131520
    /**
     * xvxkrw8652@sandbox.com
     *
     * 跳到 支付宝的二维码收银台
     * @param orderId
     * @return
     */
    @ResponseBody
    @GetMapping("/alipay/submit/{orderId}")
    public String alipayPage(@PathVariable("orderId") Long orderId) throws AlipayApiException {
        String content = alipayService.getAlipayPageHtml(orderId);

        return content;
    }

    /**
     * 跳到支付成功页
     * @return
     */
    @GetMapping("/paySuccess")
    public String paySuccess(@RequestParam Map<String,String> params) throws AlipayApiException {
        System.out.println("支付成功同步通知页，收到的参数："+params);
        //验签
        boolean b = alipayService.rsaCheckV1(params);
        if (b){
            //验签通过
//            System.out.println("修改订单状态: "+params);
        }

        return "redirect:http://gmall.com/pay/success.html";
    }

    /**
     * 支付成功，异步通知
     * @return
     */
    @ResponseBody
    @RequestMapping("/success/notify")
    public String successNotify(@RequestParam Map<String,String> params) throws AlipayApiException {
        //验签
        boolean b = alipayService.rsaCheckV1(params);
        if (b){
            //验签通过
            log.info("异步通知抵达，支付成功，验签通过，数据：{}", Jsons.toStr(params));
            //修改订单状态，用到支付宝最大努力通知
            alipayService.sendPayedMsg(params);
        }else {
            return "error";
        }
        return "success";
    }
}
