package com.atguigu.gmall.web.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.feign.cart.CartFeignClient;
import com.atguigu.gmall.model.product.SkuInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CartController {
    @Autowired
    CartFeignClient cartFeignClient;

//    public static final ThreadLocal<String> threadLocal = new ThreadLocal<>();

    //addCart.html?skuId=49&skuNum=1&sourceType=query
    /**
     * 添加商品到购物车
     * @param skuId
     * @param skuNum
     * @param model
     * @return
     */
    @GetMapping("/addCart.html")
    public String addCartHtml(@RequestParam("skuId") Long skuId,
                              @RequestParam("skuNum") Integer skuNum,
                              Model model){
        //SpringMVC每次收到请求以后，这个请求默认就和线程绑定好了

        Result<Object> result = cartFeignClient.addToCart(skuId, skuNum);
        if (result.isOk()){
            model.addAttribute("skuInfo",result.getData());
            model.addAttribute("skuNum",skuNum);
            return "cart/addCart";
        }else {
            String message = result.getMessage();
            model.addAttribute("msg",result.getData());
            return "cart/error";
        }
    }

    /**
     * 购物车列表页
     * @return
     */
    @GetMapping("/cart.html")
    public String cartHtml(){
        return "cart/index";
    }

    /**
     * 删除购物车中选中商品
     * @return
     */
    @GetMapping("/cart/deleteChecked")
    public String deleteChecked(){
        /**
         * redirect:重定向
         * forward:转发
         */
        cartFeignClient.deleteChecked();
        return "redirect:http://cart.gmall.com/cart.html";
    }
}
