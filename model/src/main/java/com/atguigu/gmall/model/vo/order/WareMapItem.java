package com.atguigu.gmall.model.vo.order;

import lombok.Data;

import java.util.List;

@Data
public class WareMapItem {
    //[{"wareId":"1","skuIds":["2","10"]},{"wareId":"2","skuIds":["3"]}]
    private Long wareId;
    private List<Long> skuIds;
}
