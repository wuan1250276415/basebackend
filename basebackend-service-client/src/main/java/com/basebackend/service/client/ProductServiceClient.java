package com.basebackend.service.client;

import com.basebackend.api.model.product.ProductDetailDTO;
import com.basebackend.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * 商品服务客户端
 */
@HttpExchange("/api/mall/products")
public interface ProductServiceClient {

    @GetExchange("/{skuId}")
    @Operation(summary = "根据SKU查询商品详情")
    Result<ProductDetailDTO> getProductDetail(@PathVariable("skuId") Long skuId);
}
