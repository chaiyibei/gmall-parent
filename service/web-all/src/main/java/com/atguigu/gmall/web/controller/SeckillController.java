package com.atguigu.gmall.web.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.feign.seckill.SeckillFeignClient;
import com.atguigu.gmall.model.activity.SeckillGoods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * 秒杀
 */
@Controller
public class SeckillController {
    @Autowired
    SeckillFeignClient seckillFeignClient;

    /**
     * 秒杀列表页
     * @return
     */
    @GetMapping("/seckill.html")
    public String seckillHtml(Model model){
        // {skuId skuDefaultImg skuName costPrice price num stockCount}
        Result<List<SeckillGoods>> goodsList = seckillFeignClient.getCurrentDaySeckillGoodsList();
        model.addAttribute("list",goodsList.getData());
        return "seckill/index";
    }

    /**
     * 商品详情页
     * @return
     */
    @GetMapping("/seckill/{skuId}.html")
    public String seckillDetail(@PathVariable("skuId")Long skuId,
                                Model model){
        Result<SeckillGoods> goods = seckillFeignClient.getSeckillGoods(skuId);
        model.addAttribute("item",goods.getData());
        return "seckill/item";
    }
}
