package com.atguigu.gmall.product.service.impl;
import com.atguigu.gmall.feign.search.SearchFeignClient;
import com.atguigu.gmall.model.list.SearchAttr;
import com.atguigu.gmall.product.service.*;
import com.google.common.collect.Lists;
import java.util.Date;

import com.atguigu.gmall.common.constant.SysRedisConst;
import com.atguigu.gmall.model.list.Goods;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.model.to.CategoryViewTo;
import com.atguigu.gmall.model.to.SkuDetailTo;
import com.atguigu.gmall.model.to.ValueSkuJsonTo;
import com.atguigu.gmall.product.mapper.*;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;

/**
* @author 柴小贝
* @description 针对表【sku_info(库存单元表)】的数据库操作Service实现
* @createDate 2022-08-23 10:13:11
*/
@Service
public class SkuInfoServiceImpl extends ServiceImpl<SkuInfoMapper, SkuInfo>
    implements SkuInfoService{
    @Autowired
    SkuInfoMapper skuInfoMapper;
    @Autowired
    SkuImageMapper skuImageMapper;
    @Autowired
    SkuAttrValueMapper skuAttrValueMapper;
    @Autowired
    SkuSaleAttrValueMapper skuSaleAttrValueMapper;
    @Autowired
    BaseCategory3Mapper baseCategory3Mapper;
    @Autowired
    SkuImageService skuImageService;
    @Autowired
    SpuSaleAttrService spuSaleAttrService;
    @Autowired
    RedissonClient redissonClient;
    @Autowired
    BaseTrademarkService baseTrademarkService;
    @Autowired
    SkuAttrValueService skuAttrValueService;
    @Autowired
    SearchFeignClient searchFeignClient;

    @Transactional
    @Override
    public void saveSkuInfo(SkuInfo skuInfo) {
        //1、保存到sku_info
        skuInfoMapper.insert(skuInfo);
        Long skuId = skuInfo.getId();

        //2、保存到sku_image
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if (!CollectionUtils.isEmpty(skuImageList)){
            for (SkuImage skuImage : skuImageList) {
                skuImage.setSkuId(skuId);
                skuImageMapper.insert(skuImage);
            }
        }

        //3、保存到sku_attr_value
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        if (!CollectionUtils.isEmpty(skuAttrValueList)){
            for (SkuAttrValue skuAttrValue : skuAttrValueList) {
                skuAttrValue.setSkuId(skuId);
                skuAttrValueMapper.insert(skuAttrValue);
            }
        }

        //4、保存到sku_sale_attr_value
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        if (!CollectionUtils.isEmpty(skuSaleAttrValueList)){
            for (SkuSaleAttrValue saleAttrValue : skuSaleAttrValueList) {
                saleAttrValue.setSpuId(skuInfo.getSpuId());
                saleAttrValue.setSkuId(skuId);
                skuSaleAttrValueMapper.insert(saleAttrValue);
            }
        }

        //把这个skuId放到布隆过滤器中
        //布隆最大的缺点：只能新增
        RBloomFilter<Object> filter = redissonClient.getBloomFilter(SysRedisConst.BLOOM_SKUID);
        filter.add(skuId);
    }

    @Override
    public void onSale(Long skuId) {
        skuInfoMapper.updateIsSale(skuId,1);
        //给es中保存这个商品，商品就能被检索到了
        Goods goods = getGoodsBySkuId(skuId);
        searchFeignClient.saveGoods(goods);
    }

    @Override
    public void cancelSale(Long skuId) {
        skuInfoMapper.updateIsSale(skuId,0);
        //从es中删除这个商品
        searchFeignClient.deleteGoods(skuId);
    }

    @Deprecated
    @Override
    public SkuDetailTo getSkuDetail(Long skuId) {
        SkuDetailTo skuDetailTo = new SkuDetailTo();
        //0、查询到skuInfo
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);

        //2、商品（sku）的基本信息【价格、重量、名字。。。】 sku_info
        skuDetailTo.setSkuInfo(skuInfo);

        //3、商品（sku）的图片 sku_image
        List<SkuImage> imageList = skuImageService.getSkuImage(skuId);
        skuInfo.setSkuImageList(imageList);

        //1、商品（sku）所属的完整分类信息 base_category1、base_category2、base_category3
        CategoryViewTo categoryViewTo = baseCategory3Mapper.getCategoryView(skuInfo.getCategory3Id());
        skuDetailTo.setCategoryView(categoryViewTo);

        //实时价格
        BigDecimal price = get1010Price(skuId);
        skuDetailTo.setPrice(price);

        //4、商品（sku）所属的spu当时定义的所有销售属性名值组合，并高亮
        //      spu_sale_attr、spu_sale_attr_value、sku_sale_attr_value
        List<SpuSaleAttr> saleAttrList = spuSaleAttrService.getSaleAttrAndValueMarkSku(skuInfo.getSpuId(),skuId);
        skuDetailTo.setSpuSaleAttrList(saleAttrList);

        // 商品（sku）的所有兄弟产品的销售属性名和值组合关系全部查出来，并封装成
        // {“118|120": "50","119|121": "50"}这样的json字符串
        Long spuId = skuInfo.getSpuId();
        String valueJson = spuSaleAttrService.getAllSkuSaleAttrValueJson(spuId);
        skuDetailTo.setValueSkuJson(valueJson);

        //5、商品（sku）类似推荐
        //6、商品（sku）介绍[所属的spu的海报] sku_poster（×）
        //7、商品（sku）的规格参数 sku_attr_value
        //8、商品（sku）的售后、评论。。。（×）
        return skuDetailTo;
    }

    @Override
    public BigDecimal get1010Price(Long skuId) {
        BigDecimal price = skuInfoMapper.getPrice(skuId);
        return price;
    }

    @Override
    public SkuInfo getDetailSkuInfo(Long skuId) {
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);
        return skuInfo;
    }

    @Override
    public List<SkuImage> getDetailSkuImages(Long skuId) {
        List<SkuImage> imageList = skuImageService.getSkuImage(skuId);
        return imageList;
    }

    @Override
    public List<Long> findAllSkuId() {
        return skuInfoMapper.getAllSkuId();
    }

    @Override
    public Goods getGoodsBySkuId(Long skuId) {
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);

        Goods goods = new Goods();
        goods.setId(skuId);
        goods.setDefaultImg(skuInfo.getSkuDefaultImg());
        goods.setTitle(skuInfo.getSkuName());
        goods.setPrice(skuInfo.getPrice().doubleValue());
        goods.setCreateTime(new Date());
        goods.setTmId(skuInfo.getTmId());

        BaseTrademark trademark = baseTrademarkService.getById(skuInfo.getTmId());
        goods.setTmName(trademark.getTmName());
        goods.setTmLogoUrl(trademark.getLogoUrl());

        Long category3Id = skuInfo.getCategory3Id();
        CategoryViewTo view = baseCategory3Mapper.getCategoryView(category3Id);
        goods.setCategory1Id(view.getCategory1Id());
        goods.setCategory1Name(view.getCategory1Name());
        goods.setCategory2Id(view.getCategory2Id());
        goods.setCategory2Name(view.getCategory2Name());
        goods.setCategory3Id(view.getCategory3Id());
        goods.setCategory3Name(view.getCategory3Name());

        goods.setHotScore(0L); //TODO 热度分更新
        //查当前sku所有平台属性名和值
        List<SearchAttr> attrs = skuAttrValueService.getSkuAttrNameAndValue(skuId);
        goods.setAttrs(attrs);

        return goods;
    }
}




