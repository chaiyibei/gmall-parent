package com.atguigu.gmall.model.to.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderMsg {
    private Long orderId;
    private Long userId;
}
