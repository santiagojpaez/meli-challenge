package com.challenge.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Productos", description = "Consulta de detalle y búsqueda de productos")
@RestController
@RequestMapping("/api/products")
@Validated
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @Operation(
        summary = "Obtener detalle de un producto",
        description = """
            Retorna el detalle completo de un producto: información básica, precio, envío \
            y atributos agrupados por sección (Características Principales, Pantalla, Batería, etc.).

            **Productos disponibles en la DB:**
            | ID | Nombre | Categoría |
            |----|--------|-----------|
            | MLA2001234567 | Apple iPhone 15 Pro Max 256 GB | Smartphones |
            | MLA1987654321 | Apple iPhone 13 128 GB | Smartphones |
            | MLA2009876543 | Samsung Galaxy S24 Ultra 256 GB | Smartphones |
            | MLA1956473829 | Samsung Galaxy A54 5G 128 GB | Smartphones |
            | MLA4578234567 | Raspberry Pi 4 Model B 8 GB | Microcontroladores |
            | MLA9274837857 | Cafetera Oster PrimaLatte | Cafeteras |
            | MLA3012345678 | Lenovo ThinkPad E14 Gen 4 | Laptops |
            | MLA3087654321 | Samsung Galaxy Tab S9 | Tablets |
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Producto encontrado",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "iPhone 15 Pro Max",
                    summary = "Detalle completo del iPhone 15 Pro Max (MLA2001234567)",
                    value = """
                        {
                          "productSummary": {
                            "id": "MLA2001234567",
                            "name": "Apple iPhone 15 Pro Max 256 GB Titanio Negro",
                            "description": "iPhone 15 Pro Max con chip A17 Pro, pantalla 6,7\\", cámara 48 MP, batería 4422 mAh.",
                            "condition": "NEW",
                            "imageUrl": "https://http2.mlstatic.com/D_NQ_NP_iphone15promax.jpg",
                            "color": "Titanio Negro",
                            "rating": 4.8,
                            "price": { "amount": 2199999.00, "originalAmount": null, "currency": "ARS" },
                            "shipping": { "freeShipping": true, "storePickup": false }
                          },
                          "weight": 221,
                          "size": "159.9 x 76.7 x 8.25 mm",
                          "availableQuantity": 45,
                          "soldQuantity": 312,
                          "category": { "id": 2, "name": "Smartphones" },
                          "attributeGroups": [
                            {
                              "groupName": "Características Principales",
                              "attributes": [
                                { "name": "Modelo", "displayValue": "iPhone 15 Pro Max" },
                                { "name": "Marca", "displayValue": "Apple" },
                                { "name": "Memoria RAM", "displayValue": "8 GB" },
                                { "name": "Almacenamiento interno", "displayValue": "256 GB" }
                              ]
                            },
                            {
                              "groupName": "Pantalla",
                              "attributes": [
                                { "name": "Tamaño de pantalla", "displayValue": "6.7 pulgadas" },
                                { "name": "Frecuencia de pantalla", "displayValue": "120 Hz" },
                                { "name": "Tipo de pantalla", "displayValue": "Super Retina XDR OLED" }
                              ]
                            },
                            {
                              "groupName": "Batería y Carga",
                              "attributes": [
                                { "name": "Capacidad de batería", "displayValue": "4422 mAh" },
                                { "name": "Carga rápida", "displayValue": "Sí" }
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
            description = "Producto no encontrado",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "timestamp": "2026-03-08T12:00:00",
                          "status": 404,
                          "error": "Not Found",
                          "message": "Product not found: MLA9999999",
                          "path": "/api/products/MLA9999999"
                        }
                        """
                )
            )
        )
    })
    @GetMapping("/{id}")
    public ProductDetailDTO getDetail(
            @Parameter(
                description = "ID del producto en formato MLA seguido de 10 dígitos.",
                example = "MLA2001234567"
            )
            @PathVariable String id) {
        return productService.getDetail(id);
    }

    @Operation(
        summary = "Buscar productos en una categoría",
        description = """
            Busca productos dentro de una categoría filtrando por nombre. \
            Soporta paginación y ordenamiento.

            **Categorías disponibles:**
            | ID | Nombre |
            |----|--------|
            | 1  | Tecnología |
            | 2  | Smartphones |
            | 3  | Electrodomésticos |
            | 4  | Cafeteras |
            | 5  | Microcontroladores |
            | 6  | Laptops |
            | 7  | Tablets |
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Página de resultados",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Búsqueda de Samsung en Smartphones",
                    summary = "GET /api/products/search?categoryId=2&q=Samsung",
                    value = """
                        {
                          "content": [
                            {
                              "id": "MLA1956473829",
                              "name": "Samsung Galaxy A54 5G 128 GB",
                              "description": "Galaxy A54 5G con pantalla 6,4\\", batería 5000 mAh, cámara 50 MP.",
                              "condition": "NEW",
                              "color": "Blanco",
                              "rating": 4.3,
                              "price": { "amount": 85000.00, "originalAmount": 95000.00, "currency": "ARS" },
                              "shipping": { "freeShipping": true, "storePickup": true }
                            },
                            {
                              "id": "MLA2009876543",
                              "name": "Samsung Galaxy S24 Ultra 256 GB",
                              "description": "Galaxy S24 Ultra con S Pen, cámara 200 MP, pantalla 6,8\\", batería 5000 mAh.",
                              "condition": "NEW",
                              "color": "Negro",
                              "rating": 4.7,
                              "price": { "amount": 1799999.00, "originalAmount": 1999999.00, "currency": "ARS" },
                              "shipping": { "freeShipping": true, "storePickup": false }
                            }
                          ],
                          "totalElements": 2,
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
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Parámetros inválidos (categoryId ≤ 0, q vacío o supera 200 caracteres)",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "timestamp": "2026-03-08T12:00:00",
                          "status": 400,
                          "error": "Bad Request",
                          "message": "search.categoryId: must be greater than 0",
                          "path": "/api/products/search"
                        }
                        """
                )
            )
        )
    })
    @GetMapping("/search")
    public Page<ProductSummaryDTO> search(
            @Parameter(
                description = "ID de la categoría donde buscar. Valores: 2=Smartphones, 4=Cafeteras, 5=Microcontroladores, 6=Laptops, 7=Tablets.",
                required = true,
                example = "2"
            )
            @RequestParam @Positive Long categoryId,
            @Parameter(
                description = "Término de búsqueda sobre el nombre del producto. Máximo 200 caracteres.",
                required = true,
                example = "Samsung"
            )
            @RequestParam @NotBlank @Size(max = 200, message = "El término de búsqueda no puede exceder 200 caracteres") String q,
            @ParameterObject @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return productService.search(categoryId, q, pageable);
    }
}
