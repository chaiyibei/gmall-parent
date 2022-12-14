package com.atguigu.gmall.order.service;

import com.atguigu.gmall.model.payment.PaymentInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

/**
* @author 柴小贝
* @description 针对表【payment_info(支付信息表)】的数据库操作Service
* @createDate 2022-09-12 14:32:08
*/
public interface PaymentInfoService extends IService<PaymentInfo> {

    /**
     * 保存支付信息
     * @param map
     * @return
     */
    PaymentInfo savePaymentInfo(Map<String, String> map);
}
