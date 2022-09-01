package com.atguigu.gmall.item.service.impl;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.Jsons;
import com.atguigu.gmall.item.feign.SkuDetailFeignClient;
import com.atguigu.gmall.item.service.SkuDetailService;
import com.atguigu.gmall.model.product.SkuImage;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.model.product.SpuSaleAttr;
import com.atguigu.gmall.model.to.CategoryViewTo;
import com.atguigu.gmall.model.to.SkuDetailTo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class SkuDetailServiceImpl implements SkuDetailService {
    @Autowired
    SkuDetailFeignClient skuDetailFeignClient;

    /**
     * 可配置的线程池，可自动注入
     */
    @Autowired
    ThreadPoolExecutor executor;

    /**
     * 本地缓存的优缺点：
     *      缺点：容量问题，数据同步问题（浪费性能），
     *      集群化后应对数据修改要通知全部微服务
     */
//    private Map<Long,SkuDetailTo> skuCache = new ConcurrentHashMap<>();

    @Autowired
    StringRedisTemplate redisTemplate;


    //未缓存优化前
//    @Override
    public SkuDetailTo getSkuDetailFromRpc(Long skuId) {
        SkuDetailTo skuDetailTo = new SkuDetailTo();
        //远程调用商品服务查询
//        Result<SkuDetailTo> skuDetail = skuDetailFeignClient.getSkuDetail(skuId);

        //CompletableFuture.runAsync()// CompletableFuture<Void>  启动一个下面不用它返回结果的异步任务
        //CompletableFuture.supplyAsync()//CompletableFuture<U>  启动一个下面用它返回结果的异步任务

        //1、查基本信息
        CompletableFuture<SkuInfo> skuInfoFuture = CompletableFuture.supplyAsync(() -> {
            Result<SkuInfo> result = skuDetailFeignClient.getSkuInfo(skuId);
            SkuInfo skuInfo = result.getData();
            skuDetailTo.setSkuInfo(skuInfo);
            return skuInfo;
        }, executor);

        //2、查商品图片信息
        CompletableFuture<Void> imageFuture = skuInfoFuture.thenAcceptAsync(skuInfo -> {
            Result<List<SkuImage>> skuImages = skuDetailFeignClient.getSkuImages(skuId);
            skuInfo.setSkuImageList(skuImages.getData());
        }, executor);

        //3、查商品实时价格
        CompletableFuture<Void> priceFuture = CompletableFuture.runAsync(() -> {
            Result<BigDecimal> price = skuDetailFeignClient.getSku1010Price(skuId);
            skuDetailTo.setPrice(price.getData());
        }, executor);

        //4、查销售属性名值
        CompletableFuture<Void> saleAttrFuture = skuInfoFuture.thenAcceptAsync(skuInfo -> {
            Result<List<SpuSaleAttr>> saleAttrValues = skuDetailFeignClient
                    .getSkuSaleAttrValues(skuId, skuInfo.getSpuId());
            skuDetailTo.setSpuSaleAttrList(saleAttrValues.getData());
        }, executor);

        //5、查sku组合
        CompletableFuture<Void> skuVlaueFuture = skuInfoFuture.thenAcceptAsync(skuInfo -> {
            Result<String> skuValueJson = skuDetailFeignClient.getSkuValueJson(skuInfo.getSpuId());
            skuDetailTo.setValueSkuJson(skuValueJson.getData());
        }, executor);

        //6、查分类
        CompletableFuture<Void> categoryFuture = skuInfoFuture.thenAcceptAsync(skuInfo -> {
            Result<CategoryViewTo> categoryView = skuDetailFeignClient.getCategoryView(skuInfo.getCategory3Id());
            skuDetailTo.setCategoryView(categoryView.getData());
        },executor);

        CompletableFuture
                .allOf(imageFuture,priceFuture,saleAttrFuture,skuVlaueFuture,categoryFuture)
                .join();

//        return skuDetail.getData();
        return skuDetailTo;
    }

    @Override
    public SkuDetailTo getSkuDetail(Long skuId) {

        //1、先看缓存中有没有 sku:info:49
        String jsonStr = redisTemplate.opsForValue().get("sku:info:" + skuId);
        if ("x".equals(jsonStr)){
            //说明以前查过，只不过数据库没有此记录，为了避免再次回源，缓存了一个占位符
            return null;
        }
        if (StringUtils.isEmpty(jsonStr)){
            //2、redis没有缓存数据
            //2.1、回源
            SkuDetailTo fromRpc = getSkuDetailFromRpc(skuId);
            //2.2、放入缓存【查到的对象转为json字符串保存到redis】
            String cacheJson = "x";
            if (fromRpc!=null){
                cacheJson = Jsons.toStr(fromRpc);
                redisTemplate.opsForValue().set("sku:info"+skuId, cacheJson,7, TimeUnit.DAYS);
            }else {
                redisTemplate.opsForValue().set("sku:info"+skuId, cacheJson,30, TimeUnit.MINUTES);
            }
            return fromRpc;
        }
        //3、缓存中有，把json转成指定的对象
        SkuDetailTo skuDetailTo = Jsons.toObj(jsonStr,SkuDetailTo.class);
        return skuDetailTo;
    }

    //使用本地缓存
//    @Override
//    public SkuDetailTo getSkuDetail(Long skuId){
//        String cacheKey = SysRedisConst.SKU_INFO_PREFIX + skuId;
//        //1、先看缓存
//        SkuDetailTo cacheData = skuCache.get(skuId);
//        //2、判断
//        if (cacheData == null){
//            //3、缓存没有，真正查询【回源（回到数据源头真正检索）】【提高缓存的命中率】
//            //预缓存机制，提升命中率至100%
//            SkuDetailTo fromRpc = getSkuDetailFromRpc(skuId);
//            skuCache.put(skuId,fromRpc);
//            return fromRpc;
//        }
//        //4、缓存有
//        return cacheData;
//    }

}
