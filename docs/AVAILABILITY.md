# Disponibilidad

Análisis de las decisiones de diseño que impactan en la disponibilidad del sistema, las queries críticas, la estrategia de índices, caché y resiliencia.

---

## Rate Limiting

La API implementa rate limiting por IP con ventana fija de 1 minuto, diferenciado por tipo de endpoint según su costo computacional.

### Configuración (`application.properties`)

| Propiedad | Valor | Descripción |
|-----------|-------|-------------|
| `rate-limit.comparisons-per-minute` | 20 | POST /api/comparisons y /api/comparisons/diff |
| `rate-limit.search-per-minute` | 60 | GET /api/products/search (full-text) |
| `rate-limit.default-per-minute` | 100 | Resto de endpoints /api/** (lecturas simples) |
| `rate-limit.window-ms` | 60000 | Duración de la ventana en milisegundos |

### Implementación

- **Filtro:** `RateLimitFilter` (extiende `OncePerRequestFilter`), actúa antes de llegar al controller.
- **Identificación:** IP del cliente, extraída de `X-Forwarded-For` (si existe) o `remoteAddr`.
- **Buckets separados:** Cada IP tiene buckets independientes por categoría (`comparisons`, `search`, `default`).
- **Headers de respuesta:** `X-RateLimit-Limit` y `X-RateLimit-Remaining` en cada response.
- **Respuesta 429:** Incluye header `Retry-After` (segundos) y cuerpo JSON con formato `ApiError`.

### Criterio de asignación de límites

| Endpoint | Límite | Justificación |
|----------|--------|---------------|
| `POST /api/comparisons{,/diff}` | 20/min | Operación más costosa: múltiples queries, resolución de atributos virtuales, cálculo de highlights |
| `GET /api/products/search` | 60/min | Full-text search con LIKE, potencialmente lento en catálogos grandes |
| `GET /api/products/{id}`, `GET /api/categories/**` | 100/min | Lecturas simples con cache habilitado |

---

## Caché con Spring Cache + Caffeine

La API usa Spring Cache con Caffeine como proveedor de caché local en memoria, configurado en `CacheConfig`.

### Caches definidos

| Cache | Servicio | TTL | Máx entradas | Justificación |
|-------|----------|-----|---------------|---------------|
| `categoryTree` | `CategoryService.getTree()` | 1 hora | 1 | Árbol estático, cambia solo con despliegues |
| `categoryDetail` | `CategoryService.getDetail()` | 1 hora | 50 | Metadatos estáticos por categoría |
| `categoryAttributes` | `CategoryService.getAttributeGroups()` | 1 hora | 50 | Reglas de atributos estáticas |
| `productDetail` | `ProductService.getDetail()` | 10 min | 200 | Datos de producto cambian poco |
| `exchangeRates` | `CurrencyExchangeService.getFactorToARS()` | 12 horas | 20 | Tasas de cambio se actualizan pocas veces al día |

---

## Circuit Breaker (Resilience4j)

La llamada a la API externa de tipo de cambio (`ExchangeRate-API`) está protegida con un circuit breaker para evitar que fallos repetidos degraden la latencia del sistema.

### Configuración (`application.properties`)

| Propiedad | Valor | Descripción |
|-----------|-------|-------------|
| `sliding-window-size` | 10 | Evalúa las últimas 10 llamadas |
| `failure-rate-threshold` | 50% | Abre el circuito si ≥50% fallan |
| `wait-duration-in-open-state` | 60s | Espera 60s antes de reintentar |
| `permitted-number-of-calls-in-half-open-state` | 3 | Permite 3 llamadas de prueba en half-open |
| `minimum-number-of-calls` | 5 | Mínimo 5 llamadas antes de evaluar umbrales |

### Comportamiento

1. **CLOSED** (normal): todas las llamadas pasan a la API externa.
2. **OPEN** (fallo detectado): las llamadas se cortocircuitan inmediatamente al `fallbackRate()` que retorna `null`. El precio queda sin normalizar pero la comparación no falla.
3. **HALF-OPEN** (después de 60s): permite 3 llamadas de prueba. Si ≥50% son exitosas, vuelve a CLOSED.

---

## Timeouts

### API externa (RestClient → ExchangeRate-API)

| Timeout | Valor | Propósito |
|---------|-------|-----------|
| Connect timeout | 5 segundos | Tiempo máximo para establecer la conexión TCP |
| Read timeout | 5 segundos | Tiempo máximo esperando la respuesta una vez conectado |

### Server-side

| Propiedad | Valor | Propósito |
|-----------|-------|-----------|
| `spring.mvc.async.request-timeout` | 30s | Timeout global para requests async del servlet |
| `server.shutdown` | graceful | No corta requests en vuelo al hacer shutdown |
| `spring.lifecycle.timeout-per-shutdown-phase` | 30s | Tiempo máximo para terminar requests activos |

### Base de datos (HikariCP + JPA)

| Propiedad | Valor | Propósito |
|-----------|-------|-----------|
| `spring.datasource.hikari.connection-timeout` | 5s | Tiempo máximo para obtener una conexión del pool |
| `spring.datasource.hikari.maximum-pool-size` | 10 | Máximo de conexiones simultáneas |
| `spring.datasource.hikari.minimum-idle` | 2 | Conexiones mínimas inactivas |
| `spring.datasource.hikari.idle-timeout` | 5 min | Tiempo antes de cerrar conexiones ociosas |
| `spring.jpa.properties.jakarta.persistence.query.timeout` | 5s | Timeout por query JPA individual |

---

## Independencia de servicios externos en tiempo de request

El endpoint de comparación (`POST /api/comparisons`) ejecuta su lógica completa contra la base de datos local. Los `normalized_value` ya están pre-calculados, los highlights se computan en memoria sobre datos ya cargados, y los atributos virtuales se resuelven desde campos de las entidades JPA.

La única dependencia externa es `CurrencyExchangeService` para normalizar precios en monedas distintas a ARS. Esta dependencia está diseñada con degradación graceful:
- Si la API key no está configurada, retorna `null` silenciosamente
- Si la llamada HTTP falla, el circuit breaker evita reintentos continuos
- El caché de Spring (Caffeine, TTL 12h) minimiza las llamadas reales

Cuando el tipo de cambio no está disponible, el precio del producto aparece en su moneda original y `normalizedValue` queda `null`. El highlight de precio se calcula solo sobre los productos que sí tienen valor normalizado. La comparación nunca falla por un problema de tipo de cambio.
