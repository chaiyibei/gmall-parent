package com.atguigu.gmall.pay.service;

import com.alipay.api.AlipayApiException;

import java.util.Map;

public interface AlipayService {
    /**
     * 生成指定订单的支付二维码页
     * @param orderId
     * @return
     */
    String getAlipayPageHtml(Long orderId) throws AlipayApiException;

    /**
     * 支付宝验签
     * @param params
     * @return
     */
    boolean rsaCheckV1(Map<String, String> params) throws AlipayApiException;

    /**
     * 发送支付成功消息给订单交换机
     * @param params
     */
    void sendPayedMsg(Map<String, String> params);
}
