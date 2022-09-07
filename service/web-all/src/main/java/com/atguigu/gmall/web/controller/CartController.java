package com.atguigu.gmall.web.controller;

import com.atguigu.gmall.feign.cart.CartFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CartController {
    @Autowired
    CartFeignClient cartFeignClient;

    @GetMapping("/addCart.html")
    public String addCarthtml(){
        return null;
    }

}
