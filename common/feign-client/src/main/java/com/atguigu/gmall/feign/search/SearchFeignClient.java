package com.atguigu.gmall.feign.search;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.list.Goods;
import com.atguigu.gmall.model.vo.search.SearchParamVo;
import com.atguigu.gmall.model.vo.search.SearchResponseVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;

@RequestMapping("/api/inner/rpc/search")
@FeignClient("service-search")
public interface SearchFeignClient {
    /**
     * 保存商品信息到es
     * @return
     */
    @PostMapping("/goods")
    Result saveGoods(@RequestBody Goods goods);

    /**
     * 删除商品信息
     * @param skuId
     * @return
     */
    @DeleteMapping("/goods/{skuId}")
    Result deleteGoods(@PathVariable("skuId") Long skuId);

    /**
     * 商品检索
     * @param paramVo
     * @return
     */
    @PostMapping("/goods/search")
    Result<SearchResponseVo> search(@RequestBody SearchParamVo paramVo);

    /**
     * 更新热度分
     * @param skuId
     * @param score
     * @return
     */
    @GetMapping("/goods/hotscore/{skuId}")
    Result updateHotScore(@PathVariable("skuId") Long skuId,
                                 @RequestParam("score") Long score
                                 );
}
