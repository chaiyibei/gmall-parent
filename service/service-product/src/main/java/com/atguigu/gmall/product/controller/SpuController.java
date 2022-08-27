package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.SpuImage;
import com.atguigu.gmall.model.product.SpuInfo;
import com.atguigu.gmall.model.product.SpuSaleAttr;
import com.atguigu.gmall.product.service.SpuImageService;
import com.atguigu.gmall.product.service.SpuInfoService;
import com.atguigu.gmall.product.service.SpuSaleAttrService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/admin/product")
@RestController
public class SpuController {
    @Autowired
    SpuInfoService spuInfoService;
    @Autowired
    SpuImageService spuImageService;

    /**
     * 获取spu分页列表
     * @param page
     * @param limit
     * @param category3Id
     * @return
     */
    @GetMapping("/{page}/{limit}")
    public Result getSPUPageList(@PathVariable("page")Long page,
                                 @PathVariable("limit")Long limit,
                                 @RequestParam("category3Id")Long category3Id){
        Page<SpuInfo> page1 = new Page<>(page,limit);
//        QueryWrapper<SpuInfo> queryWrapper = new QueryWrapper<>();
//        queryWrapper.eq("category3_id",category3Id);
//        Page<SpuInfo> spuInfoPage = spuInfoService.page(page1, queryWrapper);
        Page<SpuInfo> spuInfoPage = spuInfoService.page(page1, new LambdaQueryWrapper<SpuInfo>()
                .eq(SpuInfo::getCategory3Id, category3Id));
        return Result.ok(spuInfoPage);
    }

    /**
     * 添加spu
     * @return
     */
    @PostMapping("/saveSpuInfo")
    public Result saveSpuInfo(@RequestBody SpuInfo spuInfo){
        spuInfoService.saveSpuInfo(spuInfo);
        return Result.ok();
    }

    /**
     * 根据spuId获取图片列表
     * @param spuId
     * @return
     */
    @GetMapping("/spuImageList/{spuId}")
    public Result getSpuImageList(@PathVariable("spuId")Long spuId){
        List<SpuImage> list = spuImageService.list(new LambdaQueryWrapper<SpuImage>()
                .eq(SpuImage::getSpuId, spuId));
        return Result.ok(list);
    }

}
