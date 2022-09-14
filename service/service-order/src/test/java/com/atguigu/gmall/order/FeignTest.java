package com.atguigu.gmall.order;

import com.atguigu.gmall.feign.ware.WareFeignClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class FeignTest {
    @Autowired
    WareFeignClient wareFeignClient;

    @Test
    public void test(){
//        String search = wareFeignClient.search("尚硅谷");
//        System.out.println(search);

        String hasStock = wareFeignClient.hasStock(43l, 2);
        System.out.println(hasStock);

        System.out.println("======================================");

        String hasStock2 = wareFeignClient.hasStock(49l, 2);
        System.out.println(hasStock2);
    }
}
