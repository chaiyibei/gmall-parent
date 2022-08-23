package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseAttrInfo;
import com.atguigu.gmall.model.product.BaseAttrValue;
import com.atguigu.gmall.product.service.BaseAttrInfoService;
import com.atguigu.gmall.product.service.BaseAttrValueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 平台属性相关API
 */
@RestController
@RequestMapping("/admin/product")
public class BaseAttrController {
    @Autowired
    BaseAttrInfoService baseAttrInfoService;
    @Autowired
    BaseAttrValueService baseAttrValueService;

    /**
     * 查询某个分类下的所有平台属性
     * @param c1Id
     * @param c2Id
     * @param c3Id
     * @return
     */
    @GetMapping("/attrInfoList/{c1Id}/{c2Id}/{c3Id}")
    public Result attrInfoList(@PathVariable("c1Id")Long c1Id,
                               @PathVariable("c2Id")Long c2Id,
                               @PathVariable("c3Id")Long c3Id){
        List<BaseAttrInfo> attrInfoList =  baseAttrInfoService.getAttrInfoListAndValueByCategoryId(c1Id,c2Id,c3Id);
        return Result.ok(attrInfoList);
    }

    /**
     * 保存属性信息
     * @return
     */
    @PostMapping("/saveAttrInfo")
    public Result saveAttrInfo(@RequestBody BaseAttrInfo info){
        baseAttrInfoService.saveAttrInfo(info);
        return Result.ok();
    }

    /**
     * 根据平台属性id，获取这个属性的所有属性值
     * @param attrId
     * @return
     */
    @GetMapping("/getAttrValueList/{attrId}")
    public Result getAttrValueList(@PathVariable("attrId")Long attrId){
        List<BaseAttrValue> attrValueList = baseAttrValueService.getAttrValueList(attrId);
        return Result.ok(attrValueList);
    }
}
