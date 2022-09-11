package com.atguigu.gmall.cart.service.impl;
import java.math.BigDecimal;

import com.atguigu.gmall.common.execption.GmallException;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.common.util.Jsons;
import com.google.common.collect.Lists;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.auth.AuthUtils;
import com.atguigu.gmall.common.constant.SysRedisConst;
import com.atguigu.gmall.feign.product.SkuProductFeignClient;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.model.vo.user.UserAuthInfo;
import org.reflections.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import springfox.documentation.spring.web.json.Json;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {
    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    SkuProductFeignClient skuProductFeignClient;

    @Autowired
    ThreadPoolExecutor executor;

    @Override
    public SkuInfo addToCart(Long skuId, Integer num) {
        //  cart:user: == hash(skuId,skuInfo)
        //1、决定购物车使用哪个键
        String cartKey = determinCartKey();

        //2、给购物车添加指定商品
        SkuInfo skuInfo = addItemToCart(skuId,num,cartKey);

        //3、购物车超时设置。自动延期。
        UserAuthInfo authInfo = AuthUtils.getCurrentAuthInfo();
        if (authInfo.getUserId() == null){
            //用户未登录状态一直操作临时购物车
            String tempKey = SysRedisConst.CART_KEY + authInfo.getUserTempId();
            //临时购物车都有过期时间，自动延期
            redisTemplate.expire(tempKey,90, TimeUnit.DAYS);
        }

        return skuInfo;
    }

    public SkuInfo addItemToCart(Long skuId, Integer num, String cartKey) {
        //拿到购物车
        BoundHashOperations<String, String, String> cart = redisTemplate.boundHashOps(cartKey);
        Boolean hasKey = cart.hasKey(skuId.toString());
        //获取当前购物车的品类数量
        Long itemSize = cart.size();

        //1、如果这个skuId之前没有添加过，就新增。还需要远程调用查询当前信息
        if (!hasKey){
            if (itemSize + 1 > SysRedisConst.CART_ITEMS_LIMIT){
                //异常机制
                throw new GmallException(ResultCodeEnum.CART_OVERFLOW);
            }

            //1.1、远程获取商品信息
            SkuInfo data = skuProductFeignClient.getSkuInfo(skuId).getData();
            //1.2、转为购物车中要保存的数据模型
            CartInfo item = converSkuInfo2CartInfo(data);
            item.setSkuNum(num);
//            Result<BigDecimal> price = skuProductFeignClient.getSku1010Price(skuId);
//            item.setSkuPrice(price.getData()); //设置实时价格
            //1.3、给redis保存起来
            cart.put(skuId.toString(), Jsons.toStr(item));
            return data;
        }else {
            //2、如果这个skuId之前有添加过，就修改skuId对应的商品的数量
            //2.1、获取实时价格
            BigDecimal price = skuProductFeignClient.getSku1010Price(skuId).getData();
            //2.2、获取原来信息
            CartInfo cartInfo = getItemFromCart(cartKey,skuId);
            //2.3、更新商品
            cartInfo.setSkuPrice(price);
            cartInfo.setSkuNum(cartInfo.getSkuNum() + num);
            cartInfo.setUpdateTime(new Date());
            //2.4、同步到redis
            cart.put(skuId.toString(), Jsons.toStr(cartInfo));
            SkuInfo skuInfo = converCartInfo2SkuInfo(cartInfo);
            return skuInfo;
        }
    }

    private SkuInfo converCartInfo2SkuInfo(CartInfo cartInfo) {
        SkuInfo skuInfo = new SkuInfo();

        skuInfo.setSkuName(cartInfo.getSkuName());
        skuInfo.setSkuDefaultImg(cartInfo.getImgUrl());
        skuInfo.setId(cartInfo.getSkuId());
        return skuInfo;
    }

    @Override
    public CartInfo getItemFromCart(String cartKey, Long skuId) {
        BoundHashOperations<String, String, String> ops = redisTemplate.boundHashOps(cartKey);
        //1、拿到购物车中指定商品的json数据
        String jsonData = ops.get(skuId.toString());
        return Jsons.toObj(jsonData,CartInfo.class);
    }

    @Override
    public List<CartInfo> getCartList(String cartKey) {
        BoundHashOperations<String, String, String> ops = redisTemplate.boundHashOps(cartKey);
        //流式编程
        List<CartInfo> infos = ops.values().stream()
                .map(str -> Jsons.toObj(str, CartInfo.class))
                .sorted(((o1, o2) -> o2.getCreateTime().compareTo(o1.getCreateTime())))
                .collect(Collectors.toList());

        //顺便把购物车中所有商品的价格再次查询一遍进行更新。 异步不保证立即执行。
        //1、老请求
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        //【异步会导致feign丢失请求】
        executor.submit(()->{
            //2、绑订请求到这个线程
            RequestContextHolder.setRequestAttributes(attributes);
            updateCartAllItemsPrice(cartKey,infos);
            //3、移除数据
            RequestContextHolder.resetRequestAttributes();
        });

//        updateCartAllItemsPrice(cartKey,infos);
        return infos;
    }

    @Override
    public void updateItemNum(Long skuId, Integer num, String cartKey) {
        //1、拿到购物车
        BoundHashOperations<String, String, String> ops = redisTemplate.boundHashOps(cartKey);

        //2、拿到商品
        CartInfo item = getItemFromCart(cartKey, skuId);
        item.setSkuNum(item.getSkuNum() + num);
        item.setUpdateTime(new Date());

        //3、保存到购物车
        ops.put(skuId.toString(),Jsons.toStr(item));
    }

    @Override
    public void updateChecked(Long skuId, Integer status, String cartKey) {
        //1、拿到购物车
        BoundHashOperations<String, String, String> ops = redisTemplate.boundHashOps(cartKey);

        //2、拿到商品
        CartInfo item = getItemFromCart(cartKey, skuId);
        item.setIsChecked(status);
        item.setUpdateTime(new Date());

        //3、保存到购物车
        ops.put(skuId.toString(),Jsons.toStr(item));
    }

    @Override
    public void deleteCartItem(Long skuId, String cartKey) {
        //1、拿到购物车
        BoundHashOperations<String, String, String> ops = redisTemplate.boundHashOps(cartKey);

        //2、删除商品
        ops.delete(skuId.toString());
    }

    @Override
    public void deleteChecked(String cartKey) {
        BoundHashOperations<String, String, String> ops = redisTemplate.boundHashOps(cartKey);

        //拿到选中的商品，并删除。收集所有选中商品的id
        List<String> ids = getCheckedItems(cartKey).stream()
                .map(cartInfo -> cartInfo.getSkuId().toString())
                .collect(Collectors.toList());
        if (ids != null && ids.size() > 0){
            ops.delete(ids.toArray());
        }
    }

    @Override
    public List<CartInfo> getCheckedItems(String cartKey) {
        List<CartInfo> cartList = getCartList(cartKey);
        List<CartInfo> checkedItems = cartList.stream()
                .filter(cartInfo -> cartInfo.getIsChecked() == 1)
                .collect(Collectors.toList());
        return checkedItems;
    }

    @Override
    public void mergeUserAndTempCart() {
        UserAuthInfo authInfo = AuthUtils.getCurrentAuthInfo();
        //1、判断是否需要合并
        if (authInfo.getUserId() != null && !StringUtils.isEmpty(authInfo.getUserTempId())){
            //2、可能需要合并
            //3、临时购物车有东西。合并后删除临时购物车
            String tempCartKey = SysRedisConst.CART_KEY + authInfo.getUserTempId();
            //3.1、获取临时购物车中所有商品
            List<CartInfo> tempCartList = getCartList(tempCartKey);
            if (tempCartList != null && tempCartList.size() > 0){
                //临时购物车有数据，需要合并
                String userCartKey = SysRedisConst.CART_KEY+authInfo.getUserId();
                for (CartInfo info : tempCartList) {
                    Long skuId = info.getSkuId();
                    Integer skuNum = info.getSkuNum();
                    addItemToCart(skuId,skuNum,userCartKey);

                    //3.2、合并成一个商品就删除一个
                    redisTemplate.opsForHash().delete(tempCartKey,skuId.toString());
                }
            }
        }
    }

    @Override
    public void updateCartAllItemsPrice(String cartKey, List<CartInfo> cartInfos) {
        BoundHashOperations<String, String, String> ops = redisTemplate.boundHashOps(cartKey);

        System.out.println("更新价格开始：" + Thread.currentThread());
        //stream 不是默认并发，默认串行。   并发：.parallel()
        cartInfos.stream()
                .forEach(cartInfo -> {
                    //1、查出最新价格
                    Result<BigDecimal> price = skuProductFeignClient.getSku1010Price(cartInfo.getSkuId());
                    //2、更新价格
                    cartInfo.setSkuPrice(price.getData());
                    cartInfo.setUpdateTime(new Date());
                    //3、更新购物车价格
                    ops.put(cartInfo.getSkuId().toString(),Jsons.toStr(cartInfo));
                });
        System.out.println("更新价格结束：" + Thread.currentThread());
    }

    /**
     * 把SkuInfo转为CartInfo
     * @param data
     * @return
     */
    private CartInfo converSkuInfo2CartInfo(SkuInfo data) {
        CartInfo cartInfo = new CartInfo();
//        cartInfo.setUserId("");
        cartInfo.setSkuId(data.getId());
//        cartInfo.setSkuNum(0);
        cartInfo.setImgUrl(data.getSkuDefaultImg());
        cartInfo.setSkuName(data.getSkuName());
        cartInfo.setIsChecked(1);
        cartInfo.setCreateTime(new Date());
        cartInfo.setUpdateTime(new Date());
        cartInfo.setCartPrice(data.getPrice());
        cartInfo.setSkuPrice(data.getPrice());
//        cartInfo.setCouponInfoList(Lists.newArrayList());
//        cartInfo.setId(0L);

        return cartInfo;
    }

    @Override
    public String determinCartKey() {
        UserAuthInfo authInfo = AuthUtils.getCurrentAuthInfo();
        String cartKey = SysRedisConst.CART_KEY;
        if (authInfo.getUserId() != null){
            //用户登录了
            cartKey = cartKey + "" + authInfo.getUserId();
        }else {
            //用户未登录用临时id
            cartKey = cartKey + "" + authInfo.getUserTempId();
        }
        return cartKey;
    }
}
