package com.basebackend.mall.product.controller;

import com.basebackend.common.model.Result;
import com.basebackend.mall.product.dto.ProductDetailDTO;
import com.basebackend.mall.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品控制器
 */
@RestController
@RequestMapping("/api/mall/products")
@Validated
@Tag(name = "商城商品", description = "商城商品基础接口")
public class ProductController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * 商品服务健康检查
     *
     * @return 健康状态
     */
    @GetMapping("/ping")
    @Operation(summary = "商品服务健康检查")
    public Result<String> ping() {
        return Result.success(productService.ping());
    }

    /**
     * 查询商品详情
     *
     * @param skuId SKU ID
     * @return 商品详情
     */
    @GetMapping("/{skuId}")
    @Operation(summary = "根据SKU查询商品详情")
    public Result<ProductDetailDTO> getProductDetail(@PathVariable @NotNull Long skuId) {
        LOGGER.info("查询商品详情，skuId={}", skuId);
        return Result.success(productService.getProductBySkuId(skuId));
    }
}
