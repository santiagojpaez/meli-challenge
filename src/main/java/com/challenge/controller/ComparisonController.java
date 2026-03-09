package com.challenge.controller;

import com.challenge.dto.ComparisonDTO;
import com.challenge.dto.ComparisonDiffDTO;
import com.challenge.dto.ComparisonRequestDTO;
import com.challenge.service.ComparisonService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Comparaciones", description = "Comparación de productos de la misma categoría o de categorías compatibles (ej: Tablets vs Smartphones)")
@RestController
@RequestMapping("/api/comparisons")
public class ComparisonController {

    private final ComparisonService comparisonService;

    public ComparisonController(ComparisonService comparisonService) {
        this.comparisonService = comparisonService;
    }

    @Operation(
        summary = "Comparar productos",
        description = """
            Compara entre 2 y 5 productos mostrando todos sus atributos comparables lado a lado. \
            Detecta qué producto "gana" en cada atributo según su estrategia (HIGHER_IS_BETTER / LOWER_IS_BETTER).

            Se pueden comparar productos de la misma categoría, o de categorías cruzadas configuradas \
            (Tablets ↔ Smartphones, Tablets ↔ Laptops).

            **Campos del cuerpo:**
            - `productIds` *(requerido)*: lista de 2 a 5 IDs de productos.
            - `focusedAttributeIds` *(opcional)*: si se indica, la comparación incluye solo esos atributos (máx. 50 IDs).

            **Algunos atributos con ID para foco:**
            | ID | Nombre |
            |----|--------|
            | 1 | Memoria RAM |
            | 2 | Almacenamiento interno |
            | 3 | Tamaño de pantalla |
            | 4 | Capacidad de batería |
            | 5 | Cámara principal |
            | 29 | Precio |
            | 30 | Descuento |
            | 31 | Envío gratis |
            | 32 | Valoración |
            """,
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "IDs de productos a comparar y atributos opcionales a enfocar",
            required = true,
            content = @Content(
                mediaType = "application/json",
                examples = {
                    @ExampleObject(
                        name = "3 smartphones",
                        summary = "iPhone 15 Pro Max vs iPhone 13 vs Galaxy S24 Ultra",
                        value = """
                            {
                              "productIds": ["MLA2001234567", "MLA1987654321", "MLA2009876543"]
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Con atributos enfocados",
                        summary = "Solo RAM y almacenamiento (attrId 1 y 2)",
                        value = """
                            {
                              "productIds": ["MLA2001234567", "MLA2009876543"],
                              "focusedAttributeIds": [1, 2]
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Comparación cruzada Tablet vs Smartphone",
                        summary = "Samsung Galaxy Tab S9 vs Apple iPhone 15 Pro Max",
                        value = """
                            {
                              "productIds": ["MLA3087654321", "MLA2001234567"]
                            }
                            """
                    )
                }
            )
        )
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Resultado de comparación con ganadores por atributo",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "iPhone 15 Pro Max vs Galaxy S24 Ultra",
                    value = """
                        {
                          "products": [
                            {
                              "id": "MLA2001234567",
                              "name": "Apple iPhone 15 Pro Max 256 GB Titanio Negro",
                              "condition": "NEW",
                              "rating": 4.8,
                              "price": { "amount": 2199999.00, "originalAmount": null, "currency": "ARS" },
                              "shipping": { "freeShipping": true, "storePickup": false }
                            },
                            {
                              "id": "MLA2009876543",
                              "name": "Samsung Galaxy S24 Ultra 256 GB",
                              "condition": "NEW",
                              "rating": 4.7,
                              "price": { "amount": 1799999.00, "originalAmount": 1999999.00, "currency": "ARS" },
                              "shipping": { "freeShipping": true, "storePickup": false }
                            }
                          ],
                          "attributeGroups": [
                            {
                              "groupName": "Características Principales",
                              "groupOrder": 0,
                              "attributes": [
                                {
                                  "attributeDefId": 1,
                                  "displayName": "Memoria RAM",
                                  "values": [
                                    { "productId": "MLA2001234567", "displayValue": "8 GB", "normalizedValue": 8.0 },
                                    { "productId": "MLA2009876543", "displayValue": "12 GB", "normalizedValue": 12.0 }
                                  ],
                                  "highlight": {
                                    "winnerIds": ["MLA2009876543"],
                                    "winnerDisplayValue": "12 GB",
                                    "reason": "HIGHER_IS_BETTER"
                                  }
                                },
                                {
                                  "attributeDefId": 2,
                                  "displayName": "Almacenamiento interno",
                                  "values": [
                                    { "productId": "MLA2001234567", "displayValue": "256 GB", "normalizedValue": 256.0 },
                                    { "productId": "MLA2009876543", "displayValue": "256 GB", "normalizedValue": 256.0 }
                                  ],
                                  "highlight": null
                                }
                              ]
                            }
                          ],
                          "missingAttributes": []
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Menos de 2 o más de 5 productos, o categorías no compatibles",
            content = @Content(
                mediaType = "application/json",
                examples = {
                    @ExampleObject(
                        name = "Validación de tamaño",
                        value = """
                            {
                              "timestamp": "2026-03-08T12:00:00",
                              "status": 400,
                              "error": "Bad Request",
                              "message": "Errores de validación: productIds: Una comparación debe incluir entre 2 y 5 productos",
                              "path": "/api/comparisons"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Categorías incompatibles",
                        value = """
                            {
                              "timestamp": "2026-03-08T12:00:00",
                              "status": 400,
                              "error": "Bad Request",
                              "message": "Los productos no pertenecen a la misma categoría ni a categorías compatibles",
                              "path": "/api/comparisons"
                            }
                            """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Uno o más productos no encontrados",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "timestamp": "2026-03-08T12:00:00",
                          "status": 404,
                          "error": "Not Found",
                          "message": "Product not found: MLA9999999",
                          "path": "/api/comparisons"
                        }
                        """
                )
            )
        )
    })
    @PostMapping
    public ComparisonDTO compare(@Valid @RequestBody ComparisonRequestDTO request) {
        return comparisonService.compare(request);
    }

    @Operation(
        summary = "Diferencias entre productos",
        description = """
            Similar a `/compare` pero retorna **solo los atributos en los que los productos difieren**. \
            Útil para ver en qué se distinguen, omitiendo características idénticas.

            Aplican las mismas reglas de validación que en `/compare`: 2-5 productos, \
            categorías compatibles y `focusedAttributeIds` opcional.
            """,
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "IDs de productos a comparar y atributos opcionales a enfocar",
            required = true,
            content = @Content(
                mediaType = "application/json",
                examples = {
                    @ExampleObject(
                        name = "Diff entre 2 smartphones",
                        summary = "iPhone 15 Pro Max vs Galaxy S24 Ultra (solo diferencias)",
                        value = """
                            {
                              "productIds": ["MLA2001234567", "MLA2009876543"]
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Diff cruzado Laptop vs Tablet",
                        summary = "Lenovo ThinkPad E14 vs Samsung Galaxy Tab S9",
                        value = """
                            {
                              "productIds": ["MLA3012345678", "MLA3087654321"]
                            }
                            """
                    )
                }
            )
        )
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Solo los atributos donde los productos difieren",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Diff iPhone 15 Pro Max vs Galaxy S24 Ultra",
                    value = """
                        {
                          "products": [
                            {
                              "id": "MLA2001234567",
                              "name": "Apple iPhone 15 Pro Max 256 GB Titanio Negro",
                              "condition": "NEW",
                              "rating": 4.8,
                              "price": { "amount": 2199999.00, "originalAmount": null, "currency": "ARS" },
                              "shipping": { "freeShipping": true, "storePickup": false }
                            },
                            {
                              "id": "MLA2009876543",
                              "name": "Samsung Galaxy S24 Ultra 256 GB",
                              "condition": "NEW",
                              "rating": 4.7,
                              "price": { "amount": 1799999.00, "originalAmount": 1999999.00, "currency": "ARS" },
                              "shipping": { "freeShipping": true, "storePickup": false }
                            }
                          ],
                          "attributeGroups": [
                            {
                              "groupName": "Características Principales",
                              "groupOrder": 0,
                              "attributes": [
                                {
                                  "attributeDefId": 1,
                                  "displayName": "Memoria RAM",
                                  "values": [
                                    { "productId": "MLA2001234567", "displayValue": "8 GB", "normalizedValue": 8.0 },
                                    { "productId": "MLA2009876543", "displayValue": "12 GB", "normalizedValue": 12.0 }
                                  ],
                                  "highlight": {
                                    "winnerIds": ["MLA2009876543"],
                                    "winnerDisplayValue": "12 GB",
                                    "reason": "HIGHER_IS_BETTER"
                                  }
                                }
                              ]
                            },
                            {
                              "groupName": "Cámara",
                              "groupOrder": 0,
                              "attributes": [
                                {
                                  "attributeDefId": 5,
                                  "displayName": "Cámara principal",
                                  "values": [
                                    { "productId": "MLA2001234567", "displayValue": "48 MP", "normalizedValue": 48.0 },
                                    { "productId": "MLA2009876543", "displayValue": "200 MP", "normalizedValue": 200.0 }
                                  ],
                                  "highlight": {
                                    "winnerIds": ["MLA2009876543"],
                                    "winnerDisplayValue": "200 MP",
                                    "reason": "HIGHER_IS_BETTER"
                                  }
                                }
                              ]
                            }
                          ]
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Menos de 2 o más de 5 productos, o categorías no compatibles",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "timestamp": "2026-03-08T12:00:00",
                          "status": 400,
                          "error": "Bad Request",
                          "message": "Los productos no pertenecen a la misma categoría ni a categorías compatibles",
                          "path": "/api/comparisons/diff"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Uno o más productos no encontrados",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "timestamp": "2026-03-08T12:00:00",
                          "status": 404,
                          "error": "Not Found",
                          "message": "Product not found: MLA9999999",
                          "path": "/api/comparisons/diff"
                        }
                        """
                )
            )
        )
    })
    @PostMapping("/diff")
    public ComparisonDiffDTO diff(@Valid @RequestBody ComparisonRequestDTO request) {
        return comparisonService.diff(request);
    }
}
