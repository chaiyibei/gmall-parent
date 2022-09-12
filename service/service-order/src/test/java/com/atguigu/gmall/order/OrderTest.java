package com.atguigu.gmall.order;

import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

@SpringBootTest
public class OrderTest {
    @Autowired
    OrderInfoMapper orderInfoMapper;

    /*
        雪花算法：
            1bit(不用) + 41bit(时间戳) + 10bit(工作机器id) + 12bit(序列号)
            = 64bit = 8kb -> Long
     */

    @Test
    public void test(){
        OrderInfo orderInfo = orderInfoMapper.selectById(205l);
        System.out.println(orderInfo);
    }
    
    @Test
    public void testSplit(){
        OrderInfo orderinfo = new OrderInfo();
        orderinfo.setUserId(1l);
        orderinfo.setTotalAmount(new BigDecimal(777));
        orderInfoMapper.insert(orderinfo);
        System.out.println("1号用户订单插入完成....去 1库1表找");

        System.out.println("============================================================");

        OrderInfo orderinfo2 = new OrderInfo();
        orderinfo2.setUserId(2l);
        orderinfo2.setTotalAmount(new BigDecimal(999));
        orderInfoMapper.insert(orderinfo2);
        System.out.println("2号用户订单插入完成....去 0库2表找");
    }
    
    @Test
    public void testQuery(){
        QueryWrapper<OrderInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id",2L);
        List<OrderInfo> orderInfos = orderInfoMapper.selectList(wrapper);
        for (OrderInfo orderInfo : orderInfos) {
            System.out.println(orderInfo.getTotalAmount());
        }

//        System.out.println("============================================================");
    }

    @Test
    public void testQueryAll(){
        List<OrderInfo> infos = orderInfoMapper.selectList(null);
        for (OrderInfo info : infos) {
            System.out.println(info.getId()+"==>"+info.getTotalAmount()+"== user:"+info.getUserId());
        }
    }
}
