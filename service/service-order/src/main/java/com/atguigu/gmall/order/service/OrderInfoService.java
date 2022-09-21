package com.atguigu.gmall.order.service;

import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.vo.order.OrderSubmitVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author 柴小贝
* @description 针对表【order_info(订单表 订单表)】的数据库操作Service
* @createDate 2022-09-12 14:32:08
*/
public interface OrderInfoService extends IService<OrderInfo> {

    /**
     * 根据页面提交的数据生成一个提交数据库的订单
     * @param submitVo
     * @return
     */
    Long saveOrder(OrderSubmitVo submitVo,String tradeNo);

    /**
     * 幂等修改订单状态
     * @param orderId
     * @param userId
     * @param closed
     * @param expected
     */
    void changeOrderStatus(Long orderId, Long userId, ProcessStatus closed, List<ProcessStatus> expected);

    /**
     * 根据对外交易号和用户id获取订单信息
     * @param outTradeNo
     * @param userId
     * @return
     */
    OrderInfo getOrderInfoByOutTradeNoAndUserId(String outTradeNo, Long userId);

    /**
     * 根据订单id和用户id获取订单信息
     * @param orderId
     * @param userId
     * @return
     */
    OrderInfo getOrderInfoByOrderIdAndUserId(Long orderId, Long userId);

    /**
     * 保存秒杀单
     * @param orderInfo
     * @return
     */
    Long submitSeckillOrder(OrderInfo orderInfo);
}
