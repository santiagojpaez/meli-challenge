# Modelo de datos

## Interpretación de negocio

El eje central es el **Producto**, que pertenece a una **Categoría**. Las categorías forman una jerarquía (árbol): categorías generales contienen subcategorías especializadas. La categoría determina qué atributos son relevantes y comparables para sus productos.

Cada producto tiene su propio **Precio** (actual, original y moneda) y configuración de **Envío** (envío gratis, retiro en tienda), ambos exclusivos de ese producto.

El motor de comparación se apoya en tres entidades coordinadas. **`AttributeDefinition`** define un atributo una sola vez para todo el sistema: su nombre, tipo de dato y estrategia de comparación (`HIGHER_IS_BETTER`, `LOWER_IS_BETTER`, `NEUTRAL`). **`CategoryAttributeRule`** conecta una categoría con sus atributos válidos, estableciendo si son obligatorios, si aparecen en la tabla de comparación y el grupo visual al que pertenecen. **`ProductAttribute`** almacena el valor real del atributo para cada producto, tanto en su forma original como normalizado a una unidad base para permitir comparaciones justas (8192 MB == 8 GB).

**Nota**: El sistema actual no modela el flujo de carga (onboarding), edición o validación de jerarquías de nuevos productos. Se asume que la estructura de estas entidades y sus relaciones se generan de manera íntegra y correcta durante el proceso externo de creación del producto.

---

## Alcance del modelo

El modelo está diseñado específicamente para el **motor de comparación de productos**. Esto implica algunas decisiones deliberadas sobre qué no modelar.

**Vendedor:** Los datos del vendedor (reputación, ubicación, tasas de respuesta, historial de ventas) son relevantes para decidir *a quién* comprarle, no para comparar *qué* se compra.

**Envío:** El modelo almacena únicamente `free_shipping` y `store_pickup` porque son los datos de envío que tienen valor comparativo directo entre productos. Los detalles de logística —modos de envío, tiempos estimados, costos, Mercado Envíos— varían por vendedor, por región y por momento del día; su inclusión añadiría complejidad sin aportar al núcleo de la comparación.

**Variaciones:** En MercadoLibre un producto puede tener múltiples variaciones (color, talle, capacidad de almacenamiento), cada una con su propio stock y precio. El modelo representa cada variación como un producto independiente. Esto simplifica la lógica de comparación a costa de no reflejar la relación entre variantes del mismo artículo base.

**Opiniones:** Se modela un atributo `rating` en el producto, el cual sirve para comparar. No se ahonda en opiniones de personas que no influyen en la comparación.

---

**`AttributeGroup`** agrupa atributos por secciones para la presentación (Características Principales, Cámara, Pantalla, Batería, etc.). No afecta la lógica de comparación, pero organiza la experiencia del usuario.

La normalización de unidades la gestiona el par **`UnitGroup`** / **`Unit`**: cada grupo reúne unidades convertibles entre sí (GB, MB, TB) con sus factores de conversión a la unidad base.

**`ComparableCategory`** registra explícitamente qué pares de categorías pueden cruzarse en una comparación. Tablets puede compararse con Smartphones y con Laptops, pero no con Cafeteras. Esto hace que la comparabilidad sea una decisión editorial deliberada y no una consecuencia automática de la taxonomía.

---

## Descripción de atributos por modelo

### `products` — Product

| Campo | Columna DB | Tipo Java | Nulable | Descripción |
|---|---|---|---|---|
| `id` | `id` | `String` | No | Clave primaria alfanumérica (max 20 chars). Definida externamente (ej. `MLA123456789`). |
| `name` | `name` | `String` | No | Nombre del producto. Max 500 chars. |
| `description` | `description` | `String` | Sí | Descripción larga. Almacenada como `TEXT`. |
| `condition` | `product_condition` | `ItemCondition` | No | Estado del artículo: `NEW`, `USED` o `REFURBISHED`. |
| `imageUrl` | `image_url` | `String` | Sí | URL de la imagen principal. Max 1000 chars. |
| `color` | `color` | `String` | Sí | Color del producto. Max 100 chars. |
| `weight` | `weight` | `BigDecimal` | Sí | Peso en la unidad que corresponda. Precisión 12,4. |
| `size` | `size` | `String` | Sí | Talla o dimensión textual. Max 100 chars. |
| `rating` | `rating` | `Double` | Sí | Puntuación media del producto (≥ 0). |
| `availableQuantity` | `available_quantity` | `Integer` | No | Stock disponible. Default 0. |
| `soldQuantity` | `sold_quantity` | `Integer` | No | Unidades vendidas. Default 0. |
| `category` | `category_id` (FK) | `Category` | No | Categoría a la que pertenece el producto. |
| `price` | `price_id` (FK) | `Price` | No | Precio del producto (1:1, cascade ALL). |
| `shipping` | `shipping_id` (FK) | `Shipping` | No | Configuración de envío (1:1, cascade ALL). |
| `attributes` | — (mappedBy) | `List<ProductAttribute>` | No | Atributos dinámicos del producto (1:N, cascade ALL). |

---

### `categories` — Category

| Campo | Columna DB | Tipo Java | Nulable | Descripción |
|---|---|---|---|---|
| `id` | `id` | `Long` | No | Clave primaria autoincremental. |
| `name` | `name` | `String` | No | Nombre de la categoría. Max 255 chars. |
| `parent` | `parent_id` (FK) | `Category` | Sí | Categoría padre. `null` indica que es una categoría raíz. |
| `children` | — (mappedBy) | `List<Category>` | No | Subcategorías directas. |
| `categoryAttributeRules` | — (mappedBy) | `List<CategoryAttributeRule>` | No | Reglas de atributos configuradas para esta categoría. |

---

### `prices` — Price

| Campo | Columna DB | Tipo Java | Nulable | Descripción |
|---|---|---|---|---|
| `id` | `id` | `Long` | No | Clave primaria autoincremental. |
| `amount` | `amount` | `BigDecimal` | No | Precio de venta actual (> 0). Precisión 19,2. |
| `originalAmount` | `original_amount` | `BigDecimal` | Sí | Precio original antes de descuento. Precisión 19,2. |
| `currency` | `currency` | `CurrencyCode` | No | Moneda del precio (e.g., `ARS`, `USD`). Max 3 chars. |

---

### `shippings` — Shipping

| Campo | Columna DB | Tipo Java | Nulable | Descripción |
|---|---|---|---|---|
| `id` | `id` | `Long` | No | Clave primaria autoincremental. |
| `freeShipping` | `free_shipping` | `Boolean` | No | Indica si el envío es gratuito. Default `false`. |
| `storePickup` | `store_pickup` | `Boolean` | Sí | Indica si permite retiro en tienda. Default `false`. |

---

### `product_attributes` — ProductAttribute

| Campo | Columna DB | Tipo Java | Nulable | Descripción |
|---|---|---|---|---|
| `id` | `id` | `Long` | No | Clave primaria autoincremental. |
| `product` | `product_id` (FK) | `Product` | No | Producto al que pertenece el atributo. |
| `attributeDefinition` | `attribute_def_id` (FK) | `AttributeDefinition` | No | Definición del atributo (qué mide, cómo comparar). |
| `rawValue` | `raw_value` | `String` | No | Valor original tal como fue ingresado (e.g., `"8 GB"`). Max 1000 chars. |
| `rawUnit` | `raw_unit` | `String` | Sí | Unidad del valor original (e.g., `"GB"`). Max 50 chars. |
| `normalizedValue` | `normalized_value` | `BigDecimal` | Sí | Valor convertido a la unidad base del grupo (e.g., `8192` MB). Precisión 19,10. |
| `normalizedUnit` | `normalized_unit_id` (FK) | `Unit` | Sí | Unidad base a la que se normalizó el valor. |
| `displayValue` | `display_value` | `String` | Sí | Representación legible para UI (e.g., `"8 GB"`). Max 255 chars. |

**Índices:** `idx_pa_product (product_id)`, `idx_pa_attribute_def (attribute_def_id)`, `idx_pa_product_attr_def (product_id, attribute_def_id)`.

---

### `attribute_definitions` — AttributeDefinition

| Campo | Columna DB | Tipo Java | Nulable | Descripción |
|---|---|---|---|---|
| `id` | `id` | `Long` | No | Clave primaria autoincremental. |
| `canonicalName` | `canonical_name` | `String` | No | Identificador único del atributo en el sistema (e.g., `"storage"`). UNIQUE. Max 100 chars. |
| `displayName` | `display_name` | `String` | No | Nombre visible en la UI (e.g., `"Almacenamiento"`). Max 255 chars. |
| `description` | `description` | `String` | Sí | Descripción del atributo. `TEXT`. |
| `comparisonStrategy` | `comparison_strategy` | `ComparisonStrategy` | No | Cómo interpretar el ganador: `HIGHER_IS_BETTER`, `LOWER_IS_BETTER`, `NEUTRAL`. |
| `dataType` | `data_type` | `AttributeDataType` | No | Tipo de dato del valor: `NUMBER`, `TEXT`, `BOOLEAN`, `ENUM`, `LIST`, `RANGE`. |
| `unitGroup` | `unit_group_id` (FK) | `UnitGroup` | Sí | Grupo de unidades al que pertenece (solo atributos numéricos con unidades). |
| `productField` | `product_field` | `ProductField` | Sí | Mapeo a un campo directo del producto (`PRICE`, `DISCOUNT`, `FREE_SHIPPING`, `RATING`). Usado para atributos "virtuales". |
| `categoryAttributeRules` | — (mappedBy) | `List<CategoryAttributeRule>` | No | Reglas de categoría donde este atributo está configurado. |

**Índices:** `idx_attr_def_product_field (product_field)`. `canonical_name` tiene índice implícito por `UNIQUE`.

---

### `category_attribute_rules` — CategoryAttributeRule

| Campo | Columna DB | Tipo Java | Nulable | Descripción |
|---|---|---|---|---|
| `id` | `id` | `Long` | No | Clave primaria autoincremental. |
| `category` | `category_id` (FK) | `Category` | No | Categoría a la que aplica esta regla. |
| `attributeDefinition` | `attribute_def_id` (FK) | `AttributeDefinition` | No | Definición del atributo que aplica. |
| `attributeGroup` | `attribute_group_id` (FK) | `AttributeGroup` | No | Grupo visual al que pertenece este atributo en la categoría. |
| `isRequired` | `is_required` | `Boolean` | No | Si `true`, el atributo es obligatorio para productos de esta categoría. Default `false`. |
| `isComparable` | `is_comparable` | `Boolean` | No | Si `true`, el atributo aparece en la tabla de comparación. Default `true`. |
| `displayOrder` | `display_order` | `Integer` | No | Orden de presentación dentro del grupo. Default `0`. |

**Índices:** `idx_car_category (category_id)`, `idx_car_attribute_def (attribute_def_id)`.

---

### `attribute_groups` — AttributeGroup

| Campo | Columna DB | Tipo Java | Nulable | Descripción |
|---|---|---|---|---|
| `id` | `id` | `Long` | No | Clave primaria autoincremental. |
| `name` | `name` | `String` | No | Nombre del grupo (e.g., `"Cámara"`, `"Pantalla"`). Max 255 chars. |

---

### `comparable_categories` — ComparableCategory

| Campo | Columna DB | Tipo Java | Nulable | Descripción |
|---|---|---|---|---|
| `id` | `id` | `Long` | No | Clave primaria autoincremental. |
| `categoryA` | `category_id_a` (FK) | `Category` | No | Primera categoría del par comparable. |
| `categoryB` | `category_id_b` (FK) | `Category` | No | Segunda categoría del par comparable. |

**Restricciones:** `UNIQUE (category_id_a, category_id_b)` — cada par solo puede existir una vez.  
**Índices:** `idx_cc_category_a`, `idx_cc_category_b`, `idx_cc_pair (category_id_a, category_id_b) UNIQUE`.

---

### `unit_groups` — UnitGroup

| Campo | Columna DB | Tipo Java | Nulable | Descripción |
|---|---|---|---|---|
| `id` | `id` | `Long` | No | Clave primaria autoincremental. |
| `name` | `name` | `String` | No | Nombre del grupo de unidades (e.g., `"digital_storage"`). UNIQUE. Max 100 chars. |
| `units` | — (mappedBy) | `List<Unit>` | No | Unidades que pertenecen a este grupo. |

---

### `units` — Unit

| Campo | Columna DB | Tipo Java | Nulable | Descripción |
|---|---|---|---|---|
| `id` | `id` | `Long` | No | Clave primaria autoincremental. |
| `unitGroup` | `unit_group_id` (FK) | `UnitGroup` | No | Grupo al que pertenece esta unidad. |
| `symbol` | `symbol` | `String` | No | Símbolo de la unidad (e.g., `"GB"`, `"MB"`). UNIQUE por grupo. Max 20 chars. |
| `conversionFactor` | `conversion_factor` | `BigDecimal` | No | Factor para convertir esta unidad a la unidad base del grupo. Precisión 19,10. |
| `isBaseUnit` | `is_base_unit` | `Boolean` | No | Si `true`, esta es la unidad base del grupo (factor = 1). Default `false`. |

**Restricciones:** `UNIQUE (unit_group_id, symbol)`.

---

## Enumeraciones

### `ItemCondition`

| Valor | Descripción |
|---|---|
| `NEW` | Artículo nuevo, sin uso. |
| `USED` | Artículo usado. |
| `REFURBISHED` | Artículo reacondicionado. |

---

### `CurrencyCode`

| Valor | Descripción |
|---|---|
| `ARS` | Peso argentino |
| `USD` | Dólar estadounidense |
| `BRL` | Real brasileño |
| `MXN` | Peso mexicano |
| `COP` | Peso colombiano |
| `PEN` | Sol peruano |
| `CLP` | Peso chileno |
| `UYU` | Peso uruguayo |

---

### `AttributeDataType`

| Valor | Descripción |
|---|---|
| `NUMBER` | Valor numérico, comparable con operadores aritméticos. |
| `TEXT` | Cadena de texto libre. |
| `BOOLEAN` | Verdadero / Falso. |
| `ENUM` | Uno de un conjunto cerrado de valores textuales. |
| `LIST` | Lista de valores. |
| `RANGE` | Rango definido por un mínimo y máximo. |

---

### `ComparisonStrategy`

| Valor | Descripción |
|---|---|
| `HIGHER_IS_BETTER` | Valores más altos son preferibles (e.g., RAM, batería). |
| `LOWER_IS_BETTER` | Valores más bajos son preferibles (e.g., precio, peso). |
| `NEUTRAL` | No hay un "ganador": se muestra el valor sin resaltar. |

---

### `ProductField`

| Valor | Campo en `Product` / `Price` / `Shipping` | Descripción |
|---|---|---|
| `PRICE` | `price.amount` | Mapea el atributo al precio de venta del producto. |
| `DISCOUNT` | Calculado desde `price.amount` y `price.originalAmount` | Porcentaje de descuento respecto al precio original. |
| `FREE_SHIPPING` | `shipping.freeShipping` | Indica si el envío es gratuito. |
| `RATING` | `product.rating` | Puntuación media del producto. |

