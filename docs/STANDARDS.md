# Convenciones y estándares

Convenciones adoptadas en el proyecto para naming, códigos HTTP, estructura de errores y modelo de datos.

---

## Naming de endpoints

Los endpoints siguen REST convencional con sustantivos en plural y sin verbos en el path:

| Patrón | Ejemplo | Operación |
|--------|---------|-----------|
| `GET /api/{recurso}` | `GET /api/categories` | Listado del recurso |
| `GET /api/{recurso}/{id}` | `GET /api/products/MLA001` | Detalle por ID |
| `GET /api/{recurso}/{id}/{sub}` | `GET /api/categories/2/products` | Sub-recurso anidado |
| `POST /api/{recurso}` | `POST /api/comparisons` | Crear / ejecutar operación |

Casos específicos:
- **Búsqueda:** `GET /api/products/search?categoryId=X&q=texto` — se usa `/search` como sub-path en lugar de query params sobre el recurso raíz, porque la búsqueda incluye lógica de subárbol de categoría que no es un filtro simple.
- **Comparación diff:** `POST /api/comparisons/diff` — variante del recurso de comparación, no un recurso separado.
- **Atributos de categoría:** `GET /api/categories/{id}/attributes` — los atributos son un sub-recurso de la categoría, no un recurso de primer nivel.

---

## Códigos HTTP

| Código | Significado en esta API | Cuándo se usa |
|--------|------------------------|---------------|
| **200** | Operación exitosa | Toda respuesta correcta (GET, POST de comparación) |
| **400** | Request inválido | JSON malformado, validaciones de Bean Validation fallidas (`@NotEmpty`, `@Size`), IDs duplicados en la comparación, parámetros fuera de rango |
| **404** | Recurso no encontrado | Producto o categoría referenciado por ID no existe en la BD |
| **422** | Entidad no procesable | El request es sintácticamente correcto y los recursos existen, pero la operación no tiene sentido semántico: comparar productos de categorías incompatibles |
| **500** | Error interno | Excepción no anticipada. El mensaje original no se expone al cliente |

**Distinción 400 vs 422:** un 400 significa "tu request está mal, corregilo". Un 422 significa "tu request está bien y los datos existen, pero lo que pedís no se puede hacer" (ej. comparar un teléfono con una cafetera). El cliente no puede corregir un 422 cambiando el formato del request; necesita cambiar los datos que envía.

**Distinción 400 vs 404:** si el request envía un ID que no existe, es un 404 (el recurso no se encuentra). Si el request envía una lista vacía o un solo ID, es un 400 (el request no cumple las precondiciones).

---

## Estructura del body de error

Todas las respuestas de error usan el DTO `ApiError`:

```json
{
  "timestamp": "2026-03-08T15:30:00.123",
  "status": 400,
  "error": "Bad Request",
  "message": "Errores de validación: productIds: Una comparación debe incluir entre 2 y 5 productos",
  "path": "/api/comparisons"
}
```

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `timestamp` | `LocalDateTime` | Momento del error |
| `status` | `int` | Código HTTP numérico |
| `error` | `String` | Frase estándar del código HTTP |
| `message` | `String` | Descripción legible del error. Para 500, siempre es un mensaje genérico sin detalles técnicos |
| `path` | `String` | URI del endpoint que falló |

Esta estructura se mantiene consistente para todos los tipos de error (400, 404, 422, 500), implementada en `GlobalExceptionHandler` con un handler por cada tipo de excepción.

---

## Convenciones de naming en el modelo de datos

### Base de datos → Java

| Contexto | Convención | Ejemplo |
|----------|-----------|---------|
| Nombres de tabla | `snake_case`, plural | `product_attributes`, `category_attribute_rules` |
| Nombres de columna | `snake_case` | `raw_value`, `normalized_unit_id`, `is_required` |
| Nombres de entidad Java | `PascalCase`, singular | `ProductAttribute`, `CategoryAttributeRule` |
| Nombres de campo Java | `camelCase` | `rawValue`, `normalizedUnitId`, `isRequired` |
| Enums en BD | `UPPER_SNAKE_CASE` (como STRING) | `HIGHER_IS_BETTER`, `NEW`, `ARS` |
| Enums en Java | `UPPER_SNAKE_CASE` | `ComparisonStrategy.HIGHER_IS_BETTER` |

### Canonical names de atributos

Los `canonical_name` de `AttributeDefinition` usan `snake_case` en minúsculas:

```
ram_memory, internal_storage, screen_size, battery_capacity,
main_camera_resolution, operating_system, brand, model_name
```

Estos nombres son la identidad estable del atributo en el sistema. Se usan en queries de búsqueda (ej. `searchByText` filtra por `canonical_name IN ('brand', 'model_name')`) y como referencia para integración con sistemas externos.

Los `display_name` son human-friendly y en español: "Memoria RAM", "Almacenamiento Interno", "Tamaño de Pantalla".

### DTOs

Los DTOs son Java records con naming en `camelCase` y sufijo `DTO`:
- Records de request: `ComparisonRequestDTO`
- Records de response: `ProductSummaryDTO`, `ComparisonDTO`
- Records anidados: `ProductSummaryDTO.PriceSummaryDTO`, `ComparisonDTO.MissingAttributeDTO`
- Records internos (mapper inputs): `MapperInputs.AttributeValueInput` — sin sufijo DTO porque no son parte de la API pública

La serialización JSON usa los nombres de los campos del record directamente (`productIds`, `displayValue`, `normalizedValue`), manteniendo `camelCase` en el contrato de la API.
