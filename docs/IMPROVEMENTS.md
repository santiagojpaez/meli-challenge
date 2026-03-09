# Mejoras para producción

Mejoras a considerar si la API pasara a producción (fuera del alcance del desafío).

---

## 1. Spring Security

**Estado actual:** Sin autenticación ni autorización; todos los endpoints son públicos.

**A incorporar:** `spring-boot-starter-security` con autenticación JWT (stateless, apto para múltiples instancias) o sesiones según el contexto. Definir roles y scopes para separar acceso de lectura del de escritura (cuando exista). Restringir los endpoints de Actuator con `management.endpoint.health.show-details=when_authorized` para que los detalles de salud solo sean visibles con credenciales de operación.

---

## 2. Actuator: métricas e info

**Estado actual:** Solo está expuesto el endpoint `/actuator/health`. No hay métricas ni info del actuator habilitados.

**A incorporar:**

- **Metrics:** Habilitar `/actuator/metrics` y exportar a Prometheus/Grafana via `micrometer-registry-prometheus`. Agregar métricas custom de negocio (ver [OBSERVABILITY.md](OBSERVABILITY.md)).
- **Info:** Habilitar y enriquecer `/actuator/info` con nombre, versión y datos de build para identificación en producción. Configurar con el plugin `spring-boot-maven-plugin` y el goal `build-info`.

---

## 3. Slugs / permalinks en productos y categorías

**Estado actual:** Productos y categorías se identifican por IDs numéricos/internos (`MLA12345`, `2`).

**A incorporar:** Un campo `slug` o `permalink` generado a partir del nombre del recurso, sanitizado (minúsculas, sin caracteres especiales, guiones como separadores). Beneficios:

- **SEO:** URLs con palabras clave mejoran el posicionamiento orgánico (`/products/apple-iphone-15-pro-max-256gb` vs `/products/MLA2001234567`).
- **UX:** URLs legibles y compartibles en redes sociales.
- **Seguridad:** evita exponer patrones de IDs secuenciales que faciliten la enumeración de recursos.

Implementación: campo `slug VARCHAR(500) UNIQUE NOT NULL` en `products` y `categories`, generado automáticamente al crear el recurso (ej. con la librería `github.slugify`), indexado para búsqueda por slug.

---

## 4. Motor de búsqueda full-text

**Estado actual:** `ProductRepository.searchByText()` usa `LIKE '%query%'` sobre el nombre del producto y subconsulta sobre `product_attributes`. Esta estrategia no aprovecha índices B-tree y escala mal en catálogos grandes.

**A incorporar según el motor de base de datos:**

- **PostgreSQL:** índices GIN con `pg_trgm` (trigrams) para búsqueda insensible a acento y orden de palabras. Alternativa más completa: columna `tsvector` con índice GIN y `tsquery` para búsqueda full-text nativa con ranking y stemming en español.
- **Elasticsearch / OpenSearch:** para catálogos muy grandes o con requisitos de relevancia avanzada (sinónimos, errores ortográficos, búsqueda fonética). El servicio de productos indexaría a Elasticsearch en background; las búsquedas no irían a la base de datos relacional.

En cualquiera de los dos casos, el contrato de la API no cambia; solo la implementación interna de `searchByText`.

---

## 5. Caché con Redis

**Estado actual:** Sin caché en los endpoints. `CurrencyExchangeService` tiene un caché en memoria (`ConcurrentHashMap`, TTL 12h) que no se comparte entre instancias.

**A incorporar:**

### Cache de comparaciones

Endpoint `POST /api/comparisons` es el más costoso (múltiples queries + highlight computation). Cachear con Redis:

- **Clave:** product IDs ordenados alfabéticamente + focused attribute IDs ordenados: `compare:MLA001:MLA002:focus:1:5` — la ordenación garantiza que distintos órdenes de envío usen la misma entrada.
- **TTL:** 5-10 minutos. Los atributos cambian poco, el precio algo más seguido.
- **Invalidación:** por evento de actualización de precio o atributo del producto; sin eventos disponibles, el TTL es la garantía de frescura.
- **Implementación:** `@Cacheable` de Spring Cache + `RedisCacheManager`. Aplica igual a `compare()` y `diff()`.

### Caché del tipo de cambio

El `ConcurrentHashMap` actual no se comparte entre instancias. Reemplazar por Redis con el mismo TTL de 12h para que todas las instancias compartan la tasa ya obtenida.

### Otros candidatos

| Recurso | Almacenamiento | TTL sugerido |
|---------|---------------|-------------|
| Árbol de categorías (`GET /api/categories`) | Caffeine (local) o Redis | 1 hora |
| Reglas de categoría (cargadas en cada comparación) | Caffeine (local) | 30 minutos |
| Detalle de producto (`GET /api/products/{id}`) | Redis | 10 minutos |

Dependencias a agregar: `spring-boot-starter-data-redis`, `spring-boot-starter-cache`.
