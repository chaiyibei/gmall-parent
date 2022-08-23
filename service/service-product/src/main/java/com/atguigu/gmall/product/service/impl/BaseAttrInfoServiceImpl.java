package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.model.product.BaseAttrInfo;
import com.atguigu.gmall.model.product.BaseAttrValue;
import com.atguigu.gmall.product.mapper.BaseAttrValueMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.product.service.BaseAttrInfoService;
import com.atguigu.gmall.product.mapper.BaseAttrInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
* @author 柴小贝
* @description 针对表【base_attr_info(属性表)】的数据库操作Service实现
* @createDate 2022-08-23 10:13:11
*/
@Service
public class BaseAttrInfoServiceImpl extends ServiceImpl<BaseAttrInfoMapper, BaseAttrInfo>
    implements BaseAttrInfoService{
    @Autowired
    BaseAttrInfoMapper baseAttrInfoMapper;
    @Autowired
    BaseAttrValueMapper baseAttrValueMapper;

    @Override
    public List<BaseAttrInfo> getAttrInfoListAndValueByCategoryId(Long c1Id, Long c2Id, Long c3Id) {
        List<BaseAttrInfo> attrInfoList = baseAttrInfoMapper.getAttrInfoListAndValueByCategoryId(c1Id, c2Id, c3Id);
        return attrInfoList;
    }

    @Override
    public void saveAttrInfo(BaseAttrInfo info) {
        if (info.getId() == null){
            //1、新增
            addAttrInfo(info);
        }else {
            //2、修改
            updateAttrInfo(info);
        }
    }

    private void updateAttrInfo(BaseAttrInfo info) {
        //2.1 修改属性名
        baseAttrInfoMapper.updateById(info);
        //2.2 修改属性名信息
        List<BaseAttrValue> attrValueList = info.getAttrValueList();
        //删除
        List<Long> vids = new ArrayList<>();
        for (BaseAttrValue attrValue : attrValueList) {
            Long id = attrValue.getId();
            if (id != null){
                vids.add(id);
            }
        }
        if (vids.size() > 0){
            //部分删除
            baseAttrValueMapper.delete(new LambdaQueryWrapper<BaseAttrValue>()
                    .eq(BaseAttrValue::getAttrId, info.getId())
                    .notIn(BaseAttrValue::getId, vids));
        }else {
            //全部删除
            baseAttrValueMapper.delete(new LambdaQueryWrapper<BaseAttrValue>()
                .eq(BaseAttrValue::getAttrId, info.getId()));
        }

        for (BaseAttrValue attrValue : attrValueList) {
            if (attrValue.getId() != null){
                //修改
                baseAttrValueMapper.updateById(attrValue);
            }
            if (attrValue.getId() == null){
                //新增
                attrValue.setAttrId(info.getId());
                baseAttrValueMapper.insert(attrValue);
            }
        }
    }

    private void addAttrInfo(BaseAttrInfo info) {
        //1、保存属性名
        baseAttrInfoMapper.insert(info);
        //拿到属性名自增id
        Long id = info.getId();

        //2、保存属性值
        List<BaseAttrValue> attrValueList = info.getAttrValueList();
        for (BaseAttrValue attrValue : attrValueList) {
            //回填上一步的自增id
            attrValue.setAttrId(id);
            baseAttrValueMapper.insert(attrValue);
        }
    }
}




