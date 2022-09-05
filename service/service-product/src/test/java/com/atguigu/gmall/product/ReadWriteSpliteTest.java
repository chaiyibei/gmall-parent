package com.atguigu.gmall.product;

import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.product.mapper.BaseTrademarkMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ReadWriteSpliteTest {

    @Autowired
    BaseTrademarkMapper baseTrademarkMapper;

    @Test
    public void testRW(){
        BaseTrademark baseTrademark = baseTrademarkMapper.selectById(4l);
        System.out.println(baseTrademark);
    }
}
