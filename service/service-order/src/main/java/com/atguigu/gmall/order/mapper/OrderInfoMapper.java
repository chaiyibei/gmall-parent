package com.atguigu.gmall.order.mapper;

import com.atguigu.gmall.model.order.OrderInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author 柴小贝
 * @description 针对表【order_info(订单表 订单表)】的数据库操作Mapper
 * @createDate 2022-09-12 14:32:08
 * @Entity com.atguigu.gmall.order.domain.OrderInfo
 */
@Mapper
public interface OrderInfoMapper extends BaseMapper<OrderInfo> {

    void updateOrderStatus(@Param("orderId") Long orderId,
                           @Param("userId") Long userId,
                           @Param("processStatus") String processStatus,
                           @Param("orderStatus") String orderStatus,
                           @Param("expects") List<String> expects);
}




