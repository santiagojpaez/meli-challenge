# Mejoras para producción

Mejoras a considerar si la API pasara a producción (fuera del alcance del desafío).

---

## 1. Spring Security y CORS

**Estado actual:** Sin autenticación ni autorización; todos los endpoints son públicos. La configuración CORS (`CorsConfig`) permite cualquier origen (`*`) con `allowCredentials=true`, pensado para desarrollo local.

**A incorporar:**

- **Autenticación:** `spring-boot-starter-security` con autenticación JWT (stateless, apto para múltiples instancias) o sesiones según el contexto. Definir roles y scopes para separar acceso de lectura del de escritura (cuando exista).
- **Actuator:** Restringir los endpoints de Actuator con `management.endpoint.health.show-details=when_authorized` para que los detalles de salud solo sean visibles con credenciales de operación. Nota: esta propiedad ya está configurada, pero sin Spring Security habilitado `when_authorized` se comporta como `never` (nadie está autenticado → nadie ve detalles).
- **CORS:** Restringir `allowedOriginPattern("*")` a los dominios concretos del frontend. La combinación actual de `allowCredentials(true)` + origen `*` permite que cualquier sitio haga requests autenticados a la API. En producción, definir una lista explícita de orígenes permitidos.
- **Gestión de secrets:** Mover la API key de ExchangeRate-API (actualmente hardcodeada en `application.properties`) a un mecanismo seguro: variables de entorno inyectadas por CI/CD, Spring Cloud Config con vault, o AWS Secrets Manager / GCP Secret Manager.

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

**Estado actual:** Spring Cache con Caffeine como proveedor local. Los caches `categoryTree`, `categoryDetail`, `categoryAttributes`, `productDetail` y `exchangeRates` están activos con TTL y tamaños configurados en `CacheConfig`. Caffeine no comparte estado entre instancias.

**A incorporar:**

### Cache de comparaciones

Endpoint `POST /api/comparisons` es el más costoso (múltiples queries + highlight computation). Cachear con Redis:

- **Clave:** product IDs ordenados alfabéticamente + focused attribute IDs ordenados: `compare:MLA001:MLA002:focus:1:5` — la ordenación garantiza que distintos órdenes de envío usen la misma entrada.
- **TTL:** 5-10 minutos. Los atributos cambian poco, el precio algo más seguido.
- **Invalidación:** por evento de actualización de precio o atributo del producto; sin eventos disponibles, el TTL es la garantía de frescura.
- **Implementación:** `@Cacheable` de Spring Cache + `RedisCacheManager`. Aplica igual a `compare()` y `diff()`.

### Migración de Caffeine a Redis para multi-instancia

Los caches locales (Caffeine) no se comparten entre réplicas. En producción con más de una instancia:

| Cache | TTL actual (Caffeine) | Almacenamiento producción |
|-------|----------------------|--------------------------|
| `categoryTree` | 1 hora | Redis |
| `categoryDetail` | 1 hora | Redis |
| `productDetail` | 10 min | Redis |
| `exchangeRates` | 12 horas | Redis |

Dependencias a agregar: `spring-boot-starter-data-redis`.

---

## 6. Configuración por perfil (dev vs prod)

**Estado actual:** Existe un único `application.properties` con configuración de desarrollo:

| Propiedad | Valor actual | Riesgo en producción |
|-----------|-------------|---------------------|
| `spring.jpa.hibernate.ddl-auto` | `create-drop` | Destruye todas las tablas al cerrar la aplicación |
| `spring.jpa.show-sql` | `true` | Genera un volumen excesivo de logs con cada query SQL |
| `spring.jpa.properties.hibernate.format_sql` | `true` | Agrega líneas extra a cada query logueada |
| `spring.h2.console.enabled` | `true` | Expone la consola H2 con acceso directo a la base de datos |

**A incorporar:** Perfiles de Spring (`application-dev.properties` + `application-prod.properties`). En producción:

```properties
# Migraciones con Flyway o Liquibase en lugar de DDL automático
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.h2.console.enabled=false
```

El `ddl-auto=create-drop` es seguro con H2 en memoria (los datos se recrean en cada arranque), pero con una base de datos persistente (PostgreSQL, MySQL) borraría todas las tablas al apagar la aplicación. Reemplazar por `validate` (verifica que el esquema coincida con las entidades) y gestionar migraciones con Flyway o Liquibase.
