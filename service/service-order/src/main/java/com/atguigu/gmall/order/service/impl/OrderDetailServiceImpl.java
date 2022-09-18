package com.atguigu.gmall.order.service.impl;

import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.order.service.OrderDetailService;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author 柴小贝
* @description 针对表【order_detail(订单明细表)】的数据库操作Service实现
* @createDate 2022-09-12 14:32:08
*/
@Service
public class OrderDetailServiceImpl extends ServiceImpl<OrderDetailMapper, OrderDetail>
    implements OrderDetailService{
    @Autowired
    OrderDetailMapper orderDetailMapper;

    @Override
    public List<OrderDetail> getOrderDetails(Long orderId, Long userId) {
        List<OrderDetail> details = orderDetailMapper.selectList(new LambdaQueryWrapper<OrderDetail>()
                .eq(OrderDetail::getOrderId, orderId)
                .eq(OrderDetail::getUserId, userId));
        return details;
    }
}




