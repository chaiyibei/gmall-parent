package com.atguigu.gmall.cart.controller;

import com.atguigu.gmall.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/cart")
@RestController
public class CartRestController {

    @GetMapping("/cartList")
    public Result cartList(){

        return Result.ok();
    }


}
