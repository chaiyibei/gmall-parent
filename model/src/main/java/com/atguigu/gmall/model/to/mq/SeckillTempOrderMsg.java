package com.atguigu.gmall.model.to.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class SeckillTempOrderMsg {
    private Long userId;
    private Long skuId;
    private String skuIdStr;
}
