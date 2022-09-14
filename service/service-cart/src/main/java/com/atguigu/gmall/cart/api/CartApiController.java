package com.atguigu.gmall.cart.api;

import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.auth.AuthUtils;
import com.atguigu.gmall.common.constant.SysRedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.model.vo.user.UserAuthInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequestMapping("/api/inner/rpc/cart")
@RestController
public class CartApiController {
    @Autowired
    CartService cartService;

    /**
     * 添加商品到购物车
     * @param skuId
     * @param num
     * @return
     *
     *  @RequestHeader(value = SysRedisConst.USERID_HEADER,required = false) String userId
     *  @RequestHeader(value = SysRedisConst.USERTEMPID_HEADER,required = false) String userTempId
     */
    @GetMapping("/addToCart")
    public Result<SkuInfo> addToCart(@RequestParam("skuId") Long skuId,
                                     @RequestParam("num") Integer num){

//        UserAuthInfo authInfo = AuthUtils.getCurrentAuthInfo();
//        log.info("用户id：{},临时id：{}",authInfo.getUserId(),authInfo.getUserTempId());

        SkuInfo skuInfo = cartService.addToCart(skuId,num);

        return Result.ok(skuInfo);
    }

    /**
     * 删除购物车中选中的商品
     * @return
     */
    @GetMapping("/deleteChecked")
    public Result deleteChecked(){
        String cartKey = cartService.determinCartKey();
        cartService.deleteChecked(cartKey);
        return Result.ok();
    }

    /**
     * 获取当前购物车选中的所有商品
     * @return
     */
    @GetMapping("/checked/list")
    public Result<List<CartInfo>> getChecked(){
        String cartKey = cartService.determinCartKey();
        List<CartInfo> checkedItems = cartService.getCheckedItems(cartKey);
        return Result.ok(checkedItems);
    }

}
