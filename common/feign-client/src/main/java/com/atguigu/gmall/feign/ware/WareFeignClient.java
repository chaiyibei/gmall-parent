package com.atguigu.gmall.feign.ware;

import com.atguigu.gmall.feign.ware.callback.WareFeignClientCallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * url 指定请求发送的绝对路径
 */
@FeignClient(value = "ware-manage",
        url = "${app.ware-url:http://localhost:9001/}",
        fallback = WareFeignClientCallback.class)
public interface WareFeignClient {

//    @ResponseBody
//    @GetMapping(value = "/all",produces = "text/html;charset=utf-8")
//    String search(@RequestParam("keyword") String keyword);

    ///hasStock?skuId=10221&num=2

    /**
     * 查询一个商品是否有库存
     * @param skuId
     * @param num
     * @return
     */
    @GetMapping("/hasStock")
    String hasStock(@RequestParam("skuId") Long skuId,
                    @RequestParam("num") Integer num);
}
