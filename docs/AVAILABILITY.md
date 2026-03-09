# Disponibilidad

Análisis de las decisiones de diseño que impactan en la disponibilidad del sistema, las queries críticas, la estrategia de índices, caché y resiliencia.

---

## Independencia de servicios externos en tiempo de request

El endpoint de comparación (`POST /api/comparisons`) ejecuta su lógica completa contra la base de datos local. Los `normalized_value` ya están pre-calculados, los highlights se computan en memoria sobre datos ya cargados, y los atributos virtuales se resuelven desde campos de las entidades JPA.

La única dependencia externa es `CurrencyExchangeService` para normalizar precios en monedas distintas a ARS. Esta dependencia está diseñada con degradación graceful:
- Si la API key no está configurada, retorna `null` silenciosamente
- Si la llamada HTTP falla, retorna `null` y loguea un warning
- El caché en memoria (TTL 12h) minimiza las llamadas reales

Cuando el tipo de cambio no está disponible, el precio del producto aparece en su moneda original y `normalizedValue` queda `null`. El highlight de precio se calcula solo sobre los productos que sí tienen valor normalizado. La comparación nunca falla por un problema de tipo de cambio.
