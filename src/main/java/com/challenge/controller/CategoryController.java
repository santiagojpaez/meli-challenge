package com.challenge.controller;

import com.challenge.dto.AttributeGroupDTO;
import com.challenge.dto.CategoryDetailDTO;
import com.challenge.dto.CategorySummaryDTO;
import com.challenge.dto.CategoryTreeDTO;
import com.challenge.dto.ProductSummaryDTO;
import com.challenge.service.CategoryService;
import com.challenge.service.ProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

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


@Tag(name = "Categorías", description = "Navegación del árbol de categorías, sus atributos y productos asociados")
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final ProductService productService;

    public CategoryController(CategoryService categoryService, ProductService productService) {
        this.categoryService = categoryService;
        this.productService = productService;
    }

    @Operation(
        summary = "Árbol de categorías",
        description = "Retorna todas las categorías en estructura jerárquica (padre → hijos). Útil para renderizar menús de navegación."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Árbol completo de categorías",
        content = @Content(
            mediaType = "application/json",
            examples = @ExampleObject(
                value = """
                    [
                      {
                        "id": 1,
                        "name": "Tecnología",
                        "children": [
                          { "id": 2, "name": "Smartphones", "children": [] },
                          { "id": 5, "name": "Microcontroladores", "children": [] },
                          { "id": 6, "name": "Laptops", "children": [] },
                          { "id": 7, "name": "Tablets", "children": [] }
                        ]
                      },
                      {
                        "id": 3,
                        "name": "Electrodomésticos",
                        "children": [
                          { "id": 4, "name": "Cafeteras", "children": [] }
                        ]
                      }
                    ]
                    """
            )
        )
    )
    @GetMapping
    public List<CategoryTreeDTO> getTree() {
        return categoryService.getTree();
    }

    @Operation(
        summary = "Detalle de una categoría",
        description = "Retorna nombre, categoría padre, categorías comparables y grupos de atributos definidos para la categoría."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Categoría encontrada",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Smartphones (id=2)",
                    value = """
                        {
                          "id": 2,
                          "name": "Smartphones",
                          "parent": { "id": 1, "name": "Tecnología" },
                          "comparableWith": [],
                          "attributeGroups": [
                            {
                              "groupId": 1,
                              "groupName": "Características Principales",
                              "displayOrder": 0,
                              "attributes": [
                                { "canonicalName": "model_name", "displayName": "Modelo", "dataType": "TEXT", "isRequired": true, "isComparable": true, "displayOrder": 0 },
                                { "canonicalName": "brand", "displayName": "Marca", "dataType": "TEXT", "isRequired": true, "isComparable": true, "displayOrder": 1 },
                                { "canonicalName": "ram_memory", "displayName": "Memoria RAM", "dataType": "NUMBER", "isRequired": true, "isComparable": true, "displayOrder": 2 },
                                { "canonicalName": "internal_storage", "displayName": "Almacenamiento interno", "dataType": "NUMBER", "isRequired": true, "isComparable": true, "displayOrder": 3 }
                              ]
                            },
                            {
                              "groupId": 3,
                              "groupName": "Pantalla",
                              "displayOrder": 0,
                              "attributes": [
                                { "canonicalName": "screen_size", "displayName": "Tamaño de pantalla", "dataType": "NUMBER", "isRequired": true, "isComparable": true, "displayOrder": 7 }
                              ]
                            }
                          ]
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Categoría no encontrada",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "timestamp": "2026-03-08T12:00:00",
                          "status": 404,
                          "error": "Not Found",
                          "message": "Category not found: 99",
                          "path": "/api/categories/99"
                        }
                        """
                )
            )
        )
    })
    @GetMapping("/{id}")
    public CategoryDetailDTO getDetail(
            @Parameter(description = "ID de la categoría. Valores: 1=Tecnología, 2=Smartphones, 3=Electrodomésticos, 4=Cafeteras, 5=Microcontroladores, 6=Laptops, 7=Tablets.", example = "2")
            @PathVariable Long id) {
        return categoryService.getDetail(id);
    }

    @Operation(
        summary = "Atributos de una categoría",
        description = "Retorna los atributos definidos para la categoría, agrupados por sección. Indica cuáles son requeridos y cuáles participan en comparaciones."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Grupos de atributos de la categoría",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Atributos de Smartphones (id=2)",
                    value = """
                        [
                          {
                            "groupId": 1,
                            "groupName": "Características Principales",
                            "displayOrder": 0,
                            "attributes": [
                              { "canonicalName": "model_name", "displayName": "Modelo", "dataType": "TEXT", "isRequired": true, "isComparable": true, "displayOrder": 0 },
                              { "canonicalName": "brand", "displayName": "Marca", "dataType": "TEXT", "isRequired": true, "isComparable": true, "displayOrder": 1 },
                              { "canonicalName": "ram_memory", "displayName": "Memoria RAM", "dataType": "NUMBER", "isRequired": true, "isComparable": true, "displayOrder": 2 },
                              { "canonicalName": "internal_storage", "displayName": "Almacenamiento interno", "dataType": "NUMBER", "isRequired": true, "isComparable": true, "displayOrder": 3 }
                            ]
                          },
                          {
                            "groupId": 5,
                            "groupName": "Batería y Carga",
                            "displayOrder": 0,
                            "attributes": [
                              { "canonicalName": "battery_capacity", "displayName": "Capacidad de batería", "dataType": "NUMBER", "isRequired": true, "isComparable": true, "displayOrder": 8 },
                              { "canonicalName": "fast_charging", "displayName": "Carga rápida", "dataType": "BOOLEAN", "isRequired": false, "isComparable": true, "displayOrder": 19 }
                            ]
                          }
                        ]
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "404", description = "Categoría no encontrada")
    })
    @GetMapping("/{id}/attributes")
    public List<AttributeGroupDTO> getAttributes(
            @Parameter(description = "ID de la categoría. Ej: 2=Smartphones, 4=Cafeteras, 5=Microcontroladores, 6=Laptops, 7=Tablets.", example = "2")
            @PathVariable Long id) {
        return categoryService.getAttributeGroups(id);
    }

    @Operation(
        summary = "Categorías comparables",
        description = """
            Retorna las categorías con las que se puede hacer una comparación cruzada.

            **Comparaciones cruzadas disponibles en la DB:**
            - Tablets (id=7) ↔ Smartphones (id=2)
            - Tablets (id=7) ↔ Laptops (id=6)

            Las demás categorías retornan lista vacía.
            """
    )
    @ApiResponse(
        responseCode = "200",
        description = "Lista de categorías comparables",
        content = @Content(
            mediaType = "application/json",
            examples = @ExampleObject(
                name = "Comparables de Tablets (id=7)",
                summary = "GET /api/categories/7/comparable-categories",
                value = """
                    [
                      { "id": 2, "name": "Smartphones" },
                      { "id": 6, "name": "Laptops" }
                    ]
                    """
            )
        )
    )
    @GetMapping("/{id}/comparable-categories")
    public List<CategorySummaryDTO> getComparableCategories(
            @Parameter(description = "ID de la categoría. Ej: 7=Tablets (tiene comparables configuradas). El resto retorna lista vacía.", example = "7")
            @PathVariable Long id) {
        return categoryService.getComparableCategories(id);
    }

    @Operation(
        summary = "Productos de una categoría",
        description = "Retorna la lista paginada de productos pertenecientes a la categoría indicada."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Página de productos",
        content = @Content(
            mediaType = "application/json",
            examples = @ExampleObject(
                name = "Productos de Cafeteras (id=4)",
                summary = "GET /api/categories/4/products",
                value = """
                    {
                      "content": [
                        {
                          "id": "MLA9274837857",
                          "name": "Cafetera Oster PrimaLatte",
                          "description": "Cafetera espresso con bomba de 15 bar y depósito de 1.5 L.",
                          "condition": "NEW",
                          "color": "Negro",
                          "rating": null,
                          "price": { "amount": 1250000.00, "originalAmount": null, "currency": "ARS" },
                          "shipping": { "freeShipping": true, "storePickup": true }
                        }
                      ],
                      "totalElements": 1,
                      "totalPages": 1,
                      "size": 10,
                      "number": 0,
                      "first": true,
                      "last": true,
                      "empty": false
                    }
                    """
            )
        )
    )
    @GetMapping("/{id}/products")
    public Page<ProductSummaryDTO> getProducts(
            @Parameter(description = "ID de la categoría. Valores: 2=Smartphones (4 productos), 4=Cafeteras (1), 5=Microcontroladores (1), 6=Laptops (1), 7=Tablets (1).", example = "2")
            @PathVariable Long id,
            @ParameterObject @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return productService.listByCategory(id, pageable);
    }
}
