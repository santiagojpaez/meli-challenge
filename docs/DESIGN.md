# Decisiones de diseño

Este documento explica las decisiones técnicas centrales del sistema, qué alternativas se descartaron y por qué. Cada sección cubre un aspecto del modelo de datos o de la lógica de negocio que no es arbitrario.

---

## Normalización de atributos

### Decisión: `normalized_value` se persiste en `product_attributes`

Cada fila de `product_attributes` almacena tres representaciones del mismo dato:

| Campo | Propósito | Ejemplo |
|-------|-----------|---------|
| `raw_value` / `raw_unit` | Valor original tal como fue cargado | `8192` / `MB` |
| `normalized_value` / `normalized_unit_id` | Valor convertido a la unidad base del grupo | `8.0` (en GB) |
| `display_value` | Texto human-friendly listo para mostrar | `8 GB` |

La normalización se ejecuta al momento de la carga del producto, no en cada request. Esto significa que la comparación de dos productos es una lectura directa de `normalized_value` sin cómputo de conversión.

**Por qué no calcular en runtime.** Si la conversión se ejecutara en cada request de comparación, cada atributo de cada producto requeriría un JOIN a `units` para recuperar el `conversion_factor`, multiplicar, y recién ahí comparar. Para 5 productos con 20 atributos cada uno, serían 100 conversiones por request. Pre-calcularlo mueve ese costo al momento de carga (que ocurre una vez) y la comparación queda como operación de lectura pura.

### Cómo funciona UnitGroup y conversion_factor

Un `UnitGroup` agrupa unidades que miden lo mismo (ej. `digital_storage` contiene GB, MB, TB). Cada `Unit` tiene un `conversion_factor` que indica cuántas unidades base equivale una unidad de ese tipo. En la base de datos:

```
digital_storage: mm (factor=1.0), cm (factor=10.0), m (factor=1000.0)
```

Cuando un producto carga `32 cm` como Alto, el sistema normaliza: `32 × 10.0 = 320 mm`. El `normalized_unit_id` apunta siempre a la unidad base (donde `is_base_unit = true`).

### Atributos sin unidad

Cuando `unit_group_id` es NULL en `AttributeDefinition`, el atributo no tiene unidad (ej. `brand`, `model_name`, `operating_system`). En estos casos `normalized_value` puede ser NULL (texto sin magnitud numérica) o contener el valor numérico directo (ej. un rating de `4.8` no necesita conversión de unidad). El `display_value` se usa tal cual.

### Display value consistente

El `display_value` se genera al momento de la carga y se persiste. Si un producto carga RAM como `8192 MB` y otro como `8 GB`, ambos terminan con `display_value = "8 GB"` porque la normalización convierte a la unidad base y el display se genera desde el valor normalizado. Esto garantiza que la tabla de comparación muestre valores legibles y consistentes sin lógica de formateo en runtime.

---

## Modelo de comparabilidad

### Decisión: tabla explícita `comparable_categories`

La comparabilidad entre categorías se define mediante una tabla de pares (`category_id_a`, `category_id_b`) con un índice único compuesto. En el seed actual:

```
Tablets ↔ Smartphones
Tablets ↔ Laptops
```

**Por qué no derivar del árbol.** La alternativa obvia sería: "dos categorías son comparables si comparten el mismo padre". Esto no funciona por dos razones:

1. **No todos los hermanos son comparables.** Smartphones y Microcontroladores son ambos hijos de Tecnología, pero compararlos no tiene sentido: no comparten atributos con significado de negocio.
2. **La comparabilidad puede cruzar ramas.** En un marketplace real, podría tener sentido comparar "Auriculares Bluetooth" (hijo de Audio) con "Auriculares Gaming" (hijo de Gaming), que están en ramas distintas del árbol.

La tabla explícita transforma la comparabilidad en una decisión editorial, independiente de la estructura jerárquica.

### Validación de compatibilidad

Cuando llega un request de comparación, `ComparisonService.validateCategoriesAreComparable()` extrae los IDs de categoría de todos los productos y verifica que cada par exista en `comparable_categories`. Si hay tres categorías distintas (A, B, C), se verifican los tres pares: A↔B, A↔C, B↔C. Si alguno falta, se lanza `CategoryMismatchException` (HTTP 422).

Si todos los productos pertenecen a la misma categoría, la validación se omite (no se requiere entrada en `comparable_categories` para comparar productos de la misma categoría).

### La comparabilidad no es transitiva

Que Tablets sea comparable con Smartphones y también con Laptops no implica que Smartphones sea comparable con Laptops. Cada par es independiente. Esto es intencional: Tablets comparte atributos con ambos (pantalla, procesador), pero Smartphones y Laptops podrían tener atributos tan distintos que la comparación sea confusa.

### Bidireccionalidad con un solo query

`ComparableCategory` almacena un solo registro por par (ej. `category_id_a=7, category_id_b=2`). El query `findPairByCategoryIds` usa una condición `OR` que busca ambas direcciones:

```sql
WHERE (category_id_a = :idA AND category_id_b = :idB)
   OR (category_id_a = :idB AND category_id_b = :idA)
```

Esto evita duplicar registros (insertar tanto Tablets→Smartphones como Smartphones→Tablets) y resuelve la simetría en un solo query.

---

## Atributos por categoría

### Decisión: `CategoryAttributeRule` como tabla de intersección

La relación entre categorías y atributos no es directa. Una `AttributeDefinition` (ej. "RAM") existe una sola vez en el sistema, pero su comportamiento varía según la categoría:

- En Smartphones, RAM es **requerido** y se muestra en el grupo "Memoria y Almacenamiento" con orden 1
- En Laptops, RAM también es requerido pero podría estar en otro grupo con otro orden
- En Cafeteras, RAM no aplica

`CategoryAttributeRule` captura esta relación con campos de configuración:

| Campo | Significado |
|-------|------------|
| `is_required` | Si el producto **debe** tener este atributo cargado |
| `is_comparable` | Si el atributo aparece en la tabla de comparación |
| `display_order` | Posición del atributo dentro de su grupo |
| `attribute_group_id` | A qué grupo visual pertenece en esta categoría |

**Por qué no campos directos en AttributeDefinition.** Si `is_required` y `display_order` vivieran en `AttributeDefinition`, serían globales: RAM sería requerido en todas las categorías o en ninguna. La tabla de intersección permite configurar el mismo atributo de forma diferente por categoría, sin duplicar la definición del atributo.


### Atributos faltantes

Si un producto no tiene un atributo que la regla de la categoría marca como requerido, el sistema **no lanza excepción**. En su lugar:

1. En la tabla de comparación, la celda queda con `displayValue = null` y `normalizedValue = null`
2. Se agrega una entrada en `missingAttributes` con el ID del producto, su nombre y el nombre del atributo faltante

Esto permite que la comparación sea siempre exitosa aunque los datos estén incompletos. El frontend puede decidir cómo mostrar las celdas vacías y opcionalmente mostrar un aviso con los atributos faltantes.

---

## Highlights

### Decisión: `comparisonStrategy` vive en `AttributeDefinition`

Cada atributo tiene una estrategia de comparación fija definida en su `AttributeDefinition`:

| Estrategia | Significado | Ejemplo |
|------------|------------|---------|
| `HIGHER_IS_BETTER` | El valor más alto gana | RAM, batería, rating |
| `LOWER_IS_BETTER` | El valor más bajo gana | precio, peso |
| `NEUTRAL` | No tiene ganador | marca, sistema operativo, color |

**Por qué no calcular en runtime.** La estrategia es una propiedad inherente del atributo: RAM siempre es mejor cuanto más alta. No depende del contexto de la comparación ni de los productos involucrados. Ponerla en la definición evita que el servicio de comparación necesite lógica condicional por tipo de atributo.

### Cálculo del ganador

`ComparisonService.computeHighlight()`:

1. Si la estrategia es `NEUTRAL`, retorna `null` (no hay highlight)
2. Filtra valores con `normalizedValue != null` (para BOOLEAN, los null se tratan como `0`)
3. Si todos los valores son iguales, retorna `null` (empate total)
4. Busca el valor máximo (para `HIGHER_IS_BETTER`) o mínimo (para `LOWER_IS_BETTER`) usando un `Comparator`
5. Puede haber múltiples ganadores si comparten el mejor valor
6. Retorna un `HighlightInput` con los IDs de los ganadores, el valor ganador y la razón ("Valor más alto" / "Valor más bajo")

---

## Atributos virtuales

### Qué son

Los atributos con `product_field` no nulo en `AttributeDefinition` son atributos virtuales. No tienen filas en `product_attributes` porque su valor se deriva de campos de la entidad `Product` o de sus relaciones:

| `ProductField` | Origen | Display | Normalización |
|----------------|--------|---------|---------------|
| `PRICE` | `product.price.amount` + `currency` | `"2199999 ARS"` | Convertido a ARS vía API de tipo de cambio |
| `DISCOUNT` | `(original - actual) / original × 100` | `"11.1%"` | Porcentaje como BigDecimal |
| `FREE_SHIPPING` | `product.shipping.freeShipping` | `"Sí"` / `"No"` | `1` / `0` |
| `RATING` | `product.rating` | `"4.8"` | Valor directo |

### Por qué no tienen filas en `product_attributes`

Precio, descuento, envío y rating ya existen como campos de `Product`, `Price` y `Shipping`. Duplicarlos en `product_attributes` introduciría riesgo de inconsistencia: si el precio cambia, habría que actualizar tanto `prices` como `product_attributes`. Los atributos virtuales resuelven esto leyendo del dato canónico y computando el display/normalización en runtime.

### Resolución en el flujo de comparación

`ComparisonService` separa el flujo:

1. Recupera las `AttributeDefinition` con `product_field != null` via `findByProductFieldIsNotNull()`
2. Para cada definición virtual, `ProductFieldResolver.resolve()` lee el campo correspondiente del `Product` y retorna un `AttributeValueInput` con display y valor normalizado
3. Los atributos virtuales se agrupan bajo el grupo "Precio y Valoración" con `groupOrder = -1` (aparecen primero)
4. Reciben highlight igual que los atributos regulares (precio usa `LOWER_IS_BETTER`, rating usa `HIGHER_IS_BETTER`)

Cuando no se puede obtener el tipo de cambio (API key ausente o fallo de red), `CurrencyExchangeService.getFactorToARS()` retorna `null`, y el precio queda con `normalizedValue = null`. El highlight se calcula solo sobre los productos que sí tienen valor normalizado.

---

## Performance y escalabilidad

### Batch queries en lugar de N+1

`ProductAttributeRepository.findByProductIdIn()` carga los atributos de todos los productos de la comparación en un solo query con un `IN` clause. Esto evita el clásico N+1: si se comparan 5 productos, se ejecuta 1 query en lugar de 5.

Lo mismo aplica para `CategoryAttributeRuleRepository.findByCategoryIdIn()` y `ProductRepository.findAllByIdIn()`. El patrón se repite consistentemente: primero se resuelven los IDs necesarios, luego se hace un solo query batch.

Los repositorios usan `@EntityGraph` para eager-loading selectivo de `price` y `shipping` en las queries de productos, evitando lazy-loading implícito que generaría queries adicionales.

### Caché

**Estado actual:** Spring Cache con Caffeine como proveedor local en memoria. `CacheConfig` define cinco caches con TTL y tamaño independientes (ver [AVAILABILITY.md](AVAILABILITY.md) § Caché). Los tipos de cambio se cachean con `@Cacheable("exchangeRates")` (TTL 12h). Los endpoints de categoría y detalle de producto tienen `@Cacheable` en sus métodos de servicio.

**Producción:** se agregaría caché a nivel de:
- **Endpoint `/api/comparisons`**: Redis con clave basada en los product IDs ordenados (ej. `compare:MLA001:MLA002:MLA003`). TTL de 5-10 minutos. Invalidación por cambio de precio o atributos del producto.
- **Tipo de cambio**: mover de Caffeine (local por instancia) a Redis para compartir las tasas ya obtenidas entre todas las instancias del servicio.

---

## Robustez

### Atributo requerido ausente

Si un producto de categoría Smartphones no tiene el atributo "RAM" cargado, la comparación **no falla**. En la tabla de comparación, la celda de RAM para ese producto muestra `null`. El DTO incluye en `missingAttributes`:

```json
{
  "productId": "MLA123",
  "productName": "iPhone sin RAM",
  "attributeDisplayName": "Memoria RAM"
}
```

El frontend decide qué hacer: mostrar "N/D", un ícono de advertencia, o simplemente una celda vacía.

### Códigos HTTP y su criterio

| Código | Excepción | Cuándo |
|--------|-----------|--------|
| **400** | `MethodArgumentNotValidException` | Bean Validation falla (`@NotEmpty`, `@Size`) |
| **400** | `HttpMessageNotReadableException` | JSON malformado o tipo incorrecto |
| **400** | `InvalidComparisonRequestException` | IDs duplicados, lista vacía post-deserialización |
| **400** | `IllegalArgumentException` | Parámetros de rango inválidos |
| **404** | `ItemNotFoundException` | Producto o categoría no existe en la BD |
| **422** | `CategoryMismatchException` | Categorías no compatibles para comparar |
| **500** | `Exception` (catch-all) | Error inesperado (no expone stack trace) |

**Criterio de distinción:**
- **400** = el request está mal formado o contiene datos que no cumplen las precondiciones sintácticas. El cliente puede corregirlo y reintentar.
- **404** = el recurso referenciado no existe. El request es sintácticamente correcto pero apunta a algo que no está.
- **422** = el request es válido y los recursos existen, pero la operación no tiene sentido semántico (comparar un smartphone con una cafetera).
- **500** = error del servidor. No se expone el mensaje original al cliente por seguridad.

### Validación temprana del tamaño de la lista

`ComparisonRequestDTO` tiene `@NotEmpty` y `@Size(min=2, max=5)` en el campo `productIds`. Estas validaciones se ejecutan por Bean Validation **antes** de que el request llegue al servicio. Si alguien envía 1 ID o 6 IDs, recibe un 400 sin que se ejecute ningún query a la base de datos.

Adicionalmente, `ComparisonService.resolveAndValidateProducts()` verifica duplicados en la lista antes de consultar la BD, lanzando `InvalidComparisonRequestException.sameProduct()`.

### Excepciones del sistema

| Excepción | Factories | Propósito |
|-----------|-----------|-----------|
| `ItemNotFoundException` | `forProduct(id)`, `forCategory(id)` | Recurso no encontrado en la BD |
| `InvalidComparisonRequestException` | `emptyList()`, `singleItem()`, `moreThanFiveItems()`, `sameProduct()` | Request de comparación estructuralmente inválido |
| `CategoryMismatchException` | `forProducts(cat1, cat2)` | Par de categorías no habilitado para comparación |

El `GlobalExceptionHandler` centraliza el mapeo excepción → HTTP status + `ApiError` JSON. Todas las respuestas de error tienen la misma estructura (`timestamp`, `status`, `error`, `message`, `path`), independientemente del tipo de error.
