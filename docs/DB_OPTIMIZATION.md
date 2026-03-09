## Queries críticas

### 1. `findByProductIdIn` (ProductAttributeRepository)

```sql
SELECT pa FROM ProductAttribute pa
  JOIN FETCH pa.attributeDefinition
  WHERE pa.product.id IN (:productIds)
```

**Por qué es crítica:** es la query con mayor volumen de datos. Para 5 productos con ~15 atributos cada uno, retorna ~75 filas con JOIN a `attribute_definitions`. Se ejecuta en cada request de comparación.

**Mitigación actual:** un solo query con `IN` clause en lugar de N queries individuales. El FETCH JOIN evita lazy-loading adicional sobre las definiciones.

### 2. `findByCategoryIdIn` (CategoryAttributeRuleRepository)

```sql
SELECT r FROM CategoryAttributeRule r
  JOIN FETCH r.attributeDefinition
  JOIN FETCH r.attributeGroup
  WHERE r.category.id IN (:categoryIds)
```

**Por qué es crítica:** trae todas las reglas de las categorías involucradas con FETCH JOIN de definición y grupo. En comparación cross-categoría, puede traer reglas de dos categorías distintas.

### 3. `findAllByIdIn` (ProductRepository)

```sql
SELECT p FROM Product p
  LEFT JOIN FETCH p.price
  LEFT JOIN FETCH p.shipping
  WHERE p.id IN (:ids)
```

**Por qué es crítica:** carga los productos con precio y envío en un solo query via `@EntityGraph`. Es el punto de entrada de la comparación.

### 4. `searchByText` (ProductRepository)

```sql
SELECT DISTINCT p FROM Product p
  JOIN FETCH p.price JOIN FETCH p.shipping
  WHERE p.category.id IN :categoryIds
  AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))
       OR EXISTS (SELECT 1 FROM ProductAttribute pa ...))
```

**Por qué es crítica:** usa `LIKE '%query%'` que no puede aprovechar un índice B-tree estándar. La subconsulta con EXISTS sobre `product_attributes` agrega costo adicional. En un catálogo grande, esta query escala mal.

**Producción:** lo mejor sería reemplazar por un motor de búsqueda full-text (Elasticsearch, PostgreSQL `tsvector`).

---

## Índices de base de datos

### Índices definidos en las entidades JPA

El modelo ya declara índices en las anotaciones `@Table(indexes = ...)`:

| Tabla | Índice | Columnas |
|-------|--------|----------|
| `product_attributes` | `idx_pa_product` | `product_id` |
| `product_attributes` | `idx_pa_attribute_def` | `attribute_def_id` |
| `category_attribute_rules` | `idx_car_category` | `category_id` |
| `category_attribute_rules` | `idx_car_attribute_def` | `attribute_def_id` |
| `comparable_categories` | `idx_cc_category_a` | `category_id_a` |
| `comparable_categories` | `idx_cc_category_b` | `category_id_b` |
| `comparable_categories` | `idx_cc_pair` | `(category_id_a, category_id_b)` UNIQUE |
| `products` | `idx_product_category` | `category_id` |
| `products` | `idx_product_condition` | `product_condition` |
| `product_attributes` | `idx_pa_product_attr_def` | `(product_id, attribute_def_id)` compuesto |
| `products` | `idx_product_name` | `LOWER(name)` (funcional) |
| `attribute_definitions` | `idx_attr_def_product_field` | `product_field` |

---

## Caché

### Estado actual

- **CurrencyExchangeService:** `@Cacheable("exchangeRates")` con Caffeine, TTL 12 horas por moneda
- **CategoryService:** `@Cacheable` en `getTree()` (TTL 1h), `getDetail()` (TTL 1h), `getAttributeGroups()` (TTL 1h)
- **ProductService:** `@Cacheable("productDetail")` en `getDetail()` (TTL 10min)
- **Configuración:** `CacheConfig` registra 5 caches nombrados con tamaño máximo independiente (ver [AVAILABILITY.md](AVAILABILITY.md) § Caché)

### Producción: Redis sobre `/api/comparisons`

**Clave:** `compare:{sorted_product_ids}` — los IDs se ordenan alfabéticamente para que `compare:MLA001:MLA002` y `compare:MLA002:MLA001` usen la misma entrada de caché.

Si el request incluye `focusedAttributeIds`, se agregan a la clave: `compare:MLA001:MLA002:focus:1:5:12`.

**TTL:** 5-10 minutos. La comparación depende de datos que cambian con poca frecuencia (atributos de producto, reglas de categoría). El precio puede cambiar más seguido, pero un TTL corto mantiene la consistencia aceptable.

**Invalidación:**
- Por cambio de precio: si un servicio de precios envía un evento de actualización, se invalidan las claves que contengan el product ID afectado
- Por cambio de atributo: mismo mecanismo
- Sin eventos disponibles: el TTL de 5 minutos es la garantía de frescura

**Implementación:** `@Cacheable` de Spring Cache con `RedisCacheManager`. Anotar `ComparisonService.compare()` y `diff()`.

### Para producción: Redis

El caché de tipos de cambio y los caches por instancia con Caffeine no se comparten entre réplicas. Para producción multi-instancia, migrar a Redis:

| Recurso | Almacenamiento actual | Almacenamiento producción | TTL |
|---------|----------------------|---------------------------|-----|
| `POST /api/comparisons` | Sin caché | Redis | 5-10 min |
| Árbol de categorías | Caffeine (local) | Redis | 1 hora |
| Detalle de producto | Caffeine (local) | Redis | 10 min |
| Tipos de cambio | Caffeine (local) | Redis | 12 horas |

---