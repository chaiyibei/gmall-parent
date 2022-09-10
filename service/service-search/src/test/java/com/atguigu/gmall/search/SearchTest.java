package com.atguigu.gmall.search;

import com.atguigu.gmall.model.vo.search.SearchParamVo;
import com.atguigu.gmall.search.service.GoodsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class SearchTest {
    @Autowired
    GoodsService goodsService;
    
    @Test
    public void test(){
        SearchParamVo vo = new SearchParamVo();
        vo.setCategory3Id(61l);
        vo.setKeyword("小米");
        goodsService.search(vo);
        
    }
}
