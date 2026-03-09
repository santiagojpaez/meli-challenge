package com.challenge.controller;

import org.springdoc.core.annotations.ParameterObject;

import com.challenge.dto.ProductDetailDTO;
import com.challenge.dto.ProductSummaryDTO;
import com.challenge.service.ProductService;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/products")
@Validated
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/{id}")
    public ProductDetailDTO getDetail(@PathVariable String id) {
        return productService.getDetail(id);
    }

    @GetMapping("/search")
    public Page<ProductSummaryDTO> search(
            @RequestParam @Positive Long categoryId,
            @RequestParam @NotBlank @Size(max = 200, message = "El término de búsqueda no puede exceder 200 caracteres") String q,
            @ParameterObject @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return productService.search(categoryId, q, pageable);
    }
}
