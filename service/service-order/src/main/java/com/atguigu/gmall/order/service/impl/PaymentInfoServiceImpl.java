package com.atguigu.gmall.order.service.impl;
import java.math.BigDecimal;
import java.util.Date;

import com.atguigu.gmall.common.util.DateUtil;
import com.atguigu.gmall.common.util.Jsons;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.order.service.OrderInfoService;
import com.atguigu.gmall.order.service.PaymentInfoService;
import com.atguigu.gmall.order.mapper.PaymentInfoMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
* @author 柴小贝
* @description 针对表【payment_info(支付信息表)】的数据库操作Service实现
* @createDate 2022-09-12 14:32:08
*/
@Service
public class PaymentInfoServiceImpl extends ServiceImpl<PaymentInfoMapper, PaymentInfo>
    implements PaymentInfoService{
    @Autowired
    OrderInfoService orderInfoService;

    @Override
    public PaymentInfo savePaymentInfo(Map<String, String> map) {
        PaymentInfo paymentInfo = new PaymentInfo();

        //对外交易号
        String outTradeNo = map.get("out_trade_no");
        paymentInfo.setOutTradeNo(map.get("out_trade_no"));
        //用户id
        paymentInfo.setUserId(Long.parseLong(outTradeNo.split("_")[1]));

        //1、新增这个支付信息之前，先确认下，是否这个单数据已经保存过了；
        PaymentInfo one = getOne(new LambdaQueryWrapper<PaymentInfo>()
                .eq(PaymentInfo::getUserId, paymentInfo.getUserId())
                .eq(PaymentInfo::getOutTradeNo, paymentInfo.getOutTradeNo()));
        if (one != null){
            return one;
        }
        //2、未保存过，就新增
        //订单id
        OrderInfo orderInfo = orderInfoService
                .getOrderInfoByOutTradeNoAndUserId(outTradeNo,paymentInfo.getUserId());
        paymentInfo.setOrderId(orderInfo.getId());
        //支付方式
        paymentInfo.setPaymentType("ALIPAY");
        paymentInfo.setTradeNo(map.get("trade_no"));
        paymentInfo.setTotalAmount(new BigDecimal(map.get("total_amount")));
        paymentInfo.setSubject(map.get("subject"));
        paymentInfo.setPaymentStatus(map.get("trade_status"));
        paymentInfo.setCreateTime(new Date());
        //
        Date callbackTime = DateUtil.parseDate(map.get("notify_time"),"yyyy-MM-dd HH:mm:ss");
        paymentInfo.setCallbackTime(callbackTime);
        paymentInfo.setCallbackContent(Jsons.toStr(map));

        //保存到数据库
        save(paymentInfo);
        return paymentInfo;
    }
}




