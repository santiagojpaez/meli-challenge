# Disponibilidad

Análisis de las decisiones de diseño que impactan en la disponibilidad del sistema, las queries críticas, la estrategia de índices, caché y resiliencia.

---

## Independencia de servicios externos en tiempo de request

El endpoint de comparación (`POST /api/comparisons`) ejecuta su lógica completa contra la base de datos local. Los `normalized_value` ya están pre-calculados, los highlights se computan en memoria sobre datos ya cargados, y los atributos virtuales se resuelven desde campos de las entidades JPA.

La única dependencia externa es `CurrencyExchangeService` para normalizar precios en monedas distintas a ARS. Esta dependencia está diseñada con degradación graceful:
- Si la API key no está configurada, retorna `null` silenciosamente
- Si la llamada HTTP falla, retorna `null` y loguea un warning
- El caché en memoria (TTL 12h) minimiza las llamadas reales

### Timeouts de la API externa

El `RestClient` que llama a ExchangeRate-API tiene timeouts explícitos:

| Timeout | Valor | Propósito |
|---------|-------|-----------|
| Connect timeout | 5 segundos | Tiempo máximo para establecer la conexión TCP |
| Read timeout | 5 segundos | Tiempo máximo esperando la respuesta una vez conectado |

En el peor caso (API externa no responde), un request de comparación con productos en moneda extranjera puede tardar hasta 10 segundos extra antes de que el timeout se dispare y el servicio retorne `null`. Esto ocurre solo en la primera llamada para esa moneda (o cuando el caché expiró); las llamadas subsiguientes usan el valor cacheado sin contactar la API externa.

Si el timeout se dispara, el comportamiento es idéntico a cuando la API falla: el precio queda en su moneda original sin `normalizedValue`.

Cuando el tipo de cambio no está disponible, el precio del producto aparece en su moneda original y `normalizedValue` queda `null`. El highlight de precio se calcula solo sobre los productos que sí tienen valor normalizado. La comparación nunca falla por un problema de tipo de cambio.
