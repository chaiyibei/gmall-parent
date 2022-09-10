package com.atguigu.gmall.item.service.impl;

import com.atguigu.gmall.common.constant.SysRedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.feign.product.SkuProductFeignClient;
import com.atguigu.gmall.feign.search.SearchFeignClient;
import com.atguigu.gmall.item.service.SkuDetailService;
import com.atguigu.gmall.model.product.SkuImage;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.model.product.SpuSaleAttr;
import com.atguigu.gmall.model.to.CategoryViewTo;
import com.atguigu.gmall.model.to.SkuDetailTo;
import com.atguigu.starter.cache.annotation.GmallCache;
import com.atguigu.starter.cache.service.CacheOpsService;
import com.atguigu.starter.cache.utils.Jsons;
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
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
public class SkuDetailServiceImpl implements SkuDetailService {
    @Autowired
    SkuProductFeignClient skuProductFeignClient;

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

    //每个skuId，关联一把自己的锁
    Map<Long,ReentrantLock> lockPool = new ConcurrentHashMap<>();
    //锁的粒度太大了，把无关的人都锁住了
    ReentrantLock lock = new ReentrantLock(); //锁的住

    @Autowired
    CacheOpsService cacheOpsService;

    @Autowired
    SearchFeignClient searchFeignClient;

    /**
     * 表达式中的params代表方法的所有参数列表
     * @param skuId
     * @return
     */
//    @Transactional
    @GmallCache(
            cacheKey = SysRedisConst.SKU_INFO_PREFIX + "#{#params[0]}",
            bloomName = SysRedisConst.BLOOM_SKUID,
            bloomValue = "#{#params[0]}",
            lockName = SysRedisConst.LOCK_SKU_DETAIL+"#{#params[0]}",
            ttl = 60*60*24*7L
    )
    @Override
    public SkuDetailTo getSkuDetail(Long skuId) {
        SkuDetailTo fromRpc = getSkuDetailFromRpc(skuId);
        return fromRpc;
    }

    @Override
    public void updateHotScore(Long skuId) {
        Long increment = redisTemplate.opsForValue()
                .increment(SysRedisConst.SKU_HOTSCORE_PREFIX + skuId);
        if (increment % 100 == 0){
            searchFeignClient.updateHotScore(skuId,increment);
        }
    }

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
            Result<SkuInfo> result = skuProductFeignClient.getSkuInfo(skuId);
            SkuInfo skuInfo = result.getData();
            skuDetailTo.setSkuInfo(skuInfo);
            return skuInfo;
        }, executor);

        //2、查商品图片信息
        CompletableFuture<Void> imageFuture = skuInfoFuture.thenAcceptAsync(skuInfo -> {
            if (skuInfo != null){
                Result<List<SkuImage>> skuImages = skuProductFeignClient.getSkuImages(skuId);
                skuInfo.setSkuImageList(skuImages.getData());
            }
        }, executor);

        //3、查商品实时价格
        CompletableFuture<Void> priceFuture = CompletableFuture.runAsync(() -> {
            Result<BigDecimal> price = skuProductFeignClient.getSku1010Price(skuId);
            skuDetailTo.setPrice(price.getData());
        }, executor);

        //4、查销售属性名值
        CompletableFuture<Void> saleAttrFuture = skuInfoFuture.thenAcceptAsync(skuInfo -> {
            if (skuInfo != null) {
                Result<List<SpuSaleAttr>> saleAttrValues = skuProductFeignClient
                        .getSkuSaleAttrValues(skuId, skuInfo.getSpuId());
                skuDetailTo.setSpuSaleAttrList(saleAttrValues.getData());
            }
        }, executor);

        //5、查sku组合
        CompletableFuture<Void> skuVlaueFuture = skuInfoFuture.thenAcceptAsync(skuInfo -> {
            if (skuInfo != null){
                Result<String> skuValueJson = skuProductFeignClient.getSkuValueJson(skuInfo.getSpuId());
                skuDetailTo.setValueSkuJson(skuValueJson.getData());
            }
        }, executor);

        //6、查分类
        CompletableFuture<Void> categoryFuture = skuInfoFuture.thenAcceptAsync(skuInfo -> {
            if (skuInfo != null){
                Result<CategoryViewTo> categoryView = skuProductFeignClient.getCategoryView(skuInfo.getCategory3Id());
                skuDetailTo.setCategoryView(categoryView.getData());
            }
        },executor);

        CompletableFuture
                .allOf(imageFuture,priceFuture,saleAttrFuture,skuVlaueFuture,categoryFuture)
                .join();

//        return skuDetail.getData();
        return skuDetailTo;
    }

//    @Override
    public SkuDetailTo getSkuDetailWithCache(Long skuId) {
        String cacheKey = SysRedisConst.SKU_INFO_PREFIX +skuId;
        //1、先查缓存
        SkuDetailTo cacheData = cacheOpsService.getCacheData(cacheKey,SkuDetailTo.class);
        //2、判断
        if (cacheData == null){
            //3、缓存没有
            //4、先问布隆，是否有这个产品
            boolean contain = cacheOpsService.bloomContains(skuId);
            if (!contain){
                //5、布隆说没有，一定没有
                log.info("[{}]商品 - 布隆判定没有，检测到隐藏的攻击风险....",skuId);
                return null;
            }
            //6、布隆说有，有可能有，就需要回源查数据
            boolean lock = cacheOpsService.tryLock(skuId);//为当前商品加自己的分布式锁。
            if (lock){
                //7、获取锁成功，查询远程
                log.info("[{}]商品 缓存未命中，布隆说有，准备回源.....",skuId);
                SkuDetailTo fromRpc = getSkuDetailFromRpc(skuId);
                //8、数据放缓存
                cacheOpsService.saveData(cacheKey,fromRpc);
                //9、解锁
                cacheOpsService.unlock(skuId);
                return fromRpc;
            }
            //10、没获取到锁
            try {
                Thread.sleep(1000);
                return cacheOpsService.getCacheData(cacheKey,SkuDetailTo.class);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        //4、缓存中有
        return cacheData;
    }

//    @Override
    public SkuDetailTo getSkuDetailXxxFuture(Long skuId) {
        //每个不同的sku，用自己专用的锁
        lockPool.put(skuId,new ReentrantLock());
        //1、先看缓存中有没有 sku:info:49
        String jsonStr = redisTemplate.opsForValue().get("sku:info:" + skuId);
        if ("x".equals(jsonStr)){
            //说明以前查过，只不过数据库没有此记录，为了避免再次回源，缓存了一个占位符
            return null;
        }
        if (StringUtils.isEmpty(jsonStr)){
            //2、redis没有缓存数据
            //2.1、回源。之前可以判断redis中保存的sku的id集合，有没有这个id
            //防止随机值穿透攻击？ 回源之前，先要用布隆/bitmap判断有没有
            //TODO 加锁解决击穿
            SkuDetailTo fromRpc = null;

//            ReentrantLock lock = new ReentrantLock(); //锁不住
            //判断锁池中是否有自己的锁
            //锁池中不存在就放一把新的锁，作为自己的锁，存在就用之前的锁
            ReentrantLock lock = lockPool.putIfAbsent(skuId, new ReentrantLock());
            boolean b = this.lock.tryLock(); //立即尝试加锁，不用等，瞬发。等待逻辑在业务上 .抢一下，不成就不用再抢了
//            boolean b = lock.tryLock(1, TimeUnit.SECONDS); //等待逻辑在锁上.1s内，CPU疯狂抢锁
            if (b){
                //抢到锁
                fromRpc = getSkuDetailFromRpc(skuId);
            }else {
                //没抢到
//                Thread.sleep(1000);
                jsonStr = redisTemplate.opsForValue().get("sku:info:" + skuId);
                //逆转为 SkuDetailTo
                return null;
            }
            //2.2、放入缓存【查到的对象转为json字符串保存到redis】
            String cacheJson = "x";
            if (fromRpc!=null){
                cacheJson = Jsons.toStr(fromRpc);
                //加入雪崩解决方案，固定业务时间+随机过期时间
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
