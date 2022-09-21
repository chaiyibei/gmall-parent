package com.atguigu.gmall.web.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.feign.seckill.SeckillFeignClient;
import com.atguigu.gmall.model.activity.SeckillGoods;
import com.atguigu.gmall.model.vo.seckill.SeckillOrderConfirmVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

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

    /**
     * 秒杀排队页
     * @return
     */
    @GetMapping("/seckill/queue.html")
    public String seckillQueue(@RequestParam("skuId") Long skuId,
                               @RequestParam("skuIdStr") String skuIdStr,
                               Model model){
        model.addAttribute("skuId",skuId);
        model.addAttribute("skuIdStr",skuIdStr);
        return "seckill/queue";
    }

    /**
     * 秒杀下单页
     * @return
     */
    @GetMapping("/seckill/trade.html")
    public String trade(Model model,
                        @RequestParam("skuId") Long skuId){
        Result<SeckillOrderConfirmVo> confirmVo =
                seckillFeignClient.getSeckillOrderConfirmVo(skuId);

        SeckillOrderConfirmVo voData = confirmVo.getData();
        //返回的是订单确认页的数据
        model.addAttribute("detailArrayList",voData.getTempOrder().getOrderDetailList());
        model.addAttribute("userAddressList",voData.getUserAddressList());
        model.addAttribute("totalNum",voData.getTempOrder().getOrderDetailList().size());
        model.addAttribute("totalAmount",voData.getTempOrder().getTotalAmount());

        return "seckill/trade";
    }
}
