package com.challenge.controller;

import com.challenge.dto.AttributeGroupDTO;
import com.challenge.dto.CategoryDetailDTO;
import com.challenge.dto.CategorySummaryDTO;
import com.challenge.dto.CategoryTreeDTO;
import com.challenge.dto.ProductSummaryDTO;
import com.challenge.service.CategoryService;
import com.challenge.service.ProductService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springdoc.core.annotations.ParameterObject;

import java.util.List;


@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final ProductService productService;

    public CategoryController(CategoryService categoryService, ProductService productService) {
        this.categoryService = categoryService;
        this.productService = productService;
    }

    @GetMapping
    public List<CategoryTreeDTO> getTree() {
        return categoryService.getTree();
    }

    @GetMapping("/{id}")
    public CategoryDetailDTO getDetail(@PathVariable Long id) {
        return categoryService.getDetail(id);
    }

    @GetMapping("/{id}/attributes")
    public List<AttributeGroupDTO> getAttributes(@PathVariable Long id) {
        return categoryService.getAttributeGroups(id);
    }

    @GetMapping("/{id}/comparable-categories")
    public List<CategorySummaryDTO> getComparableCategories(@PathVariable Long id) {
        return categoryService.getComparableCategories(id);
    }

    @GetMapping("/{id}/products")
    public Page<ProductSummaryDTO> getProducts(@PathVariable Long id, @ParameterObject @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return productService.listByCategory(id, pageable);
    }
}
