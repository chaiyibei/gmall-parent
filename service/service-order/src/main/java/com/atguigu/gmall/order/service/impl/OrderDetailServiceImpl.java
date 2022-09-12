package com.atguigu.gmall.order.service.impl;

import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.order.service.OrderDetailService;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
* @author 柴小贝
* @description 针对表【order_detail(订单明细表)】的数据库操作Service实现
* @createDate 2022-09-12 14:32:08
*/
@Service
public class OrderDetailServiceImpl extends ServiceImpl<OrderDetailMapper, OrderDetail>
    implements OrderDetailService{

}




