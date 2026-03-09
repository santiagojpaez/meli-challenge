# Observabilidad

Estado actual de la observabilidad del sistema y plan de mejora ante salida a producción.

---

## Estado actual

### Spring Actuator

La configuración de Actuator en `application.properties`:

```properties
management.endpoints.web.exposure.include=health
management.endpoint.health.show-details=when_authorized
management.endpoint.health.show-components=always
```

**Endpoints expuestos:**

| Endpoint | Qué devuelve |
|----------|-------------|
| `GET /actuator/health` | Estado de la aplicación con componentes individuales |

El endpoint de health incluye dos indicadores:
1. **db** — health check automático de Spring Data JPA que verifica conectividad a la base de datos H2
2. **meliChallengeApi** — indicador custom (`ActuatorHealthConfig`) que retorna `UP` con metadata del componente. Actualmente estático, diseñado como punto de extensión para agregar checks de dependencias externas (API de tipo de cambio, etc.)

**Nota sobre `show-details=when_authorized`:** Sin Spring Security habilitado, esta propiedad se comporta como `never` — nadie está autenticado, por lo tanto nadie ve los `withDetail(...)` de cada componente. El response muestra los componentes (porque `show-components=always`) pero solo con su `status`, sin detalles internos como `"component": "meli-challenge-api"` del health indicator custom.

Respuesta ejemplo de `/actuator/health` (sin autenticación):
```json
{
  "status": "UP",
  "components": {
    "actuatorHealthConfig": {
      "status": "UP"
    },
    "db": {
      "status": "UP"
    },
    "diskSpace": {
      "status": "UP"
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

### Logging actual

El proyecto usa SLF4J vía Lombok (`@Slf4j`) con niveles estándar de Spring Boot (Logback). Los logs relevantes:

| Componente | Nivel | Qué loguea |
|-----------|-------|------------|
| `GlobalExceptionHandler` | WARN | Errores de validación, comparaciones inválidas, categorías incompatibles |
| `GlobalExceptionHandler` | INFO | Recursos no encontrados (404) |
| `GlobalExceptionHandler` | ERROR | Excepciones no manejadas (500), con stack trace |
| `GlobalExceptionHandler` | DEBUG | Errores de parsing JSON |
| `CurrencyExchangeService` | WARN | Fallos al obtener tipo de cambio |
| `CurrencyExchangeService` | DEBUG | Caché de tipo de cambio, API key no configurada |

**Estado actual:** los logs se emiten en formato texto plano (default de Logback). No hay correlationId, requestId ni campos estructurados.

**Nota:** `spring.jpa.show-sql=true` y `format_sql=true` están habilitados en la configuración actual, lo que produce un log por cada query SQL ejecutada. Esto es útil en desarrollo para depurar, pero en producción generaría un volumen excesivo de logs y debería desactivarse (ver [IMPROVEMENTS.md](IMPROVEMENTS.md) § Configuración por perfil).

---

## Producción — lo que se agregaría

### Métricas de negocio con Micrometer

Spring Boot incluye Micrometer en el starter de Actuator, pero actualmente no se instrumentan métricas custom. Se podrían agregar:

| Métrica | Tipo | Propósito |
|---------|------|-----------|
| `comparisons.total` | Counter (tag: `categoryId`) | Cantidad de comparaciones por categoría, para entender qué categorías generan más tráfico |
| `comparisons.product_count` | Distribution Summary | Distribución de cuántos productos se comparan por request (¿la mayoría compara 2 o 5?) |
| `comparisons.duration` | Timer | Tiempo de resolución del endpoint de comparación, incluyendo query + highlight |
| `comparisons.cross_category` | Counter | Comparaciones que cruzan categorías vs misma categoría |
| `products.search.total` | Counter (tag: `categoryId`) | Búsquedas de productos por categoría |
| `exchange_rate.fetch.total` | Counter (tags: `currency`, `result=success\|failure`) | Llamadas a la API de tipo de cambio |
| `exchange_rate.cache.hit_ratio` | Gauge | Efectividad del caché de tipo de cambio |

Implementación: inyectar `MeterRegistry` en los servicios y registrar métricas con `registry.counter()`, `registry.timer()`, etc. Exportar a Prometheus con `micrometer-registry-prometheus`.

### Logs estructurados

Reemplazar el formato texto plano por JSON estructurado para que sea parseable por herramientas de log aggregation (ELK, Loki, Datadog):

```json
{
  "timestamp": "2026-03-08T15:30:00.000Z",
  "level": "INFO",
  "logger": "ComparisonService",
  "message": "Comparison completed",
  "requestId": "abc-123",
  "productIds": ["MLA001", "MLA002"],
  "categoryIds": [2],
  "productCount": 2,
  "durationMs": 45,
  "crossCategory": false
}
```

### Alertas

| Alerta | Condición | Severidad |
|--------|-----------|-----------|
| Latencia de comparación | p99 de `comparisons.duration` > 2s por 5 min | Warning |
| Latencia de comparación | p99 de `comparisons.duration` > 5s por 5 min | Critical |
| Tasa de errores 422 | Ratio 422/total > 20% en ventana de 10 min | Warning (puede indicar un bug en el frontend que envía categorías incompatibles) |
| Tasa de errores 500 | Cualquier 500 | Critical |
| API de tipo de cambio | `exchange_rate.fetch.total{result=failure}` > 3 en 1 hora | Warning |
| Health check DOWN | `/actuator/health` retorna status != UP | Critical |
