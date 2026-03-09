# Meli Challenge API

API REST de consulta y comparación de productos, construida con Spring Boot 3. El sistema modela un catálogo donde cada producto pertenece a una categoría, posee atributos normalizados y puede compararse lado a lado con otros productos de categorías compatibles. La comparación produce una tabla agrupada por tipo de atributo, con highlights automáticos que señalan al ganador según la estrategia de cada atributo (mayor es mejor, menor es mejor, o neutral).

El alcance del proyecto está acotado intencionalmente a la consulta y comparación: no expone endpoints de creación ni administración de productos, categorías o atributos. Esta decisión respeta la consigna del challenge, que pone el foco en el motor de comparación. La carga inicial del catálogo se resuelve mediante datos de seed en SQL, lo que permite evaluar el sistema con un conjunto representativo de productos sin necesidad de una capa de ingesta. Se asume que dicha capa de ingesta es responsable de las validaciones, conversiones de unidades y resolución de sinónimos necesarias para que los datos persistan en el formato que el modelo requiere.

El modelo de datos está diseñado para ser extensible sin cambios de código: agregar una categoría nueva con sus atributos, reglas de comparabilidad y productos se resuelve enteramente con INSERTs en la base de datos. Los atributos se normalizan a una unidad base al momento de la carga, lo que permite comparar valores heterogéneos (ej. 8 GB vs 8192 MB) sin cómputo en tiempo de request.

El proyecto incluye documentación OpenAPI auto-generada, manejo centralizado de errores con respuestas JSON estandarizadas, y un pipeline de tests en tres niveles: unitarios (servicios con mocks), de capa web (controllers con MockMvc) y de integración completa (stack real contra H2).

---

## Requisitos previos

| Requisito | Versión |
|-----------|---------|
| Java (JDK) | 21+ |
| Maven | 3.8+ |
| Base de datos | Ninguna — H2 embebido en memoria |

No se requiere instalar ni configurar ninguna base de datos. El esquema y los datos iniciales se crean automáticamente al arrancar la aplicación mediante JPA DDL y `data.sql`.

---

## Setup local

```bash
# 1. Compilar (descarga dependencias en la primera ejecución)
mvn clean compile

# 2. Levantar la aplicación
mvn spring-boot:run
```

La aplicación queda disponible en `http://localhost:8080`.

---

## Tests

```bash
# Ejecutar todos los tests
mvn test

# Ejecutar tests con reporte de cobertura (JaCoCo)
mvn verify
# Reporte HTML en: target/site/jacoco/index.html
```

El proyecto tiene cuatro niveles de tests:
- **Unitarios de servicio** — `CategoryServiceTest`, `ProductServiceTest` (Mockito, sin contexto Spring)
- **De controller** — `CategoryControllerTest`, `ProductControllerTest`, `ComparisonControllerTest` (`@WebMvcTest` con MockMvc)
- **De integración** — `ApiIntegrationTest` (`@SpringBootTest` contra H2 con data.sql real)
- **De repositorio** — `RepositoryTest` (`@DataJpaTest` con datos manuales)

---

## Documentación OpenAPI

Con el servidor corriendo:

| Recurso | URL |
|---------|-----|
| Swagger UI (interactivo) | [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html) |
| OpenAPI spec (JSON) | [http://localhost:8080/api-docs](http://localhost:8080/api-docs) |

El spec cubre todos los endpoints, parámetros, DTOs de request/response y códigos de error.

---

## Variables de entorno

| Variable | Descripción | Valor por defecto |
|----------|-------------|-------------------|
| `exchange.rate.api.key` | API key para [ExchangeRate-API](https://www.exchangerate-api.com) (normalización de precios a ARS) | `ddbd00d2ca13738057686c62` |
| `exchange.rate.api.base-url` | URL base de la API de tipo de cambio | `https://v6.exchangerate-api.com/v6` |

Si `exchange.rate.api.key` está vacía o ausente, la normalización de precios se deshabilita silenciosamente y el precio aparece en su moneda original sin valor normalizado. El resto de la aplicación funciona sin degradación.

---

## Estructura de paquetes

```
com.challenge
├── config/         Configuración de OpenAPI y health indicators de Actuator
├── controller/     Endpoints REST (categorías, productos, comparaciones)
├── dto/            Records de transferencia: requests, responses y errores
├── exception/      Excepciones de negocio y handler global (@RestControllerAdvice)
├── mapper/         Conversión entidad → DTO (clases estáticas, sin framework)
├── model/          Entidades JPA, enums del dominio
├── repository/     Interfaces Spring Data JPA con queries JPQL/nativas
└── service/        Lógica de negocio: comparación, normalización, resolución de atributos virtuales
```

---

## Funcionalidades del sistema

### Catálogo de categorías
- Árbol jerárquico de categorías (padre → hijos)
- Detalle de categoría con atributos definidos, agrupados y ordenados
- Listado de categorías comparables con la categoría consultada

### Catálogo de productos
- Listado paginado de productos por categoría
- Búsqueda full-text por nombre, marca y modelo dentro del subárbol de una categoría
- Detalle de producto con atributos agrupados y valores de display

### Comparación de productos
- Comparación completa de 2 a 5 productos: tabla agrupada con highlight de ganadores. Permite especificar atributos a incluir en la comparación
- Modo diff: solo atributos donde los productos difieren
- Soporte para comparación cross-categoría (Tablets vs Smartphones, por ejemplo) validada contra tabla de compatibilidad
- Atributos virtuales calculados en runtime: precio normalizado a ARS, descuento porcentual, envío gratis, rating
- Detección de atributos requeridos faltantes por producto

### Flujo esperado

1. El cliente consulta `GET /api/categories` para obtener el árbol de categorías
2. Navega a una categoría y lista productos con `GET /api/categories/{id}/products`
3. Opcionalmente busca productos con `GET /api/products/search?categoryId=X&q=iphone`
4. Selecciona 2-5 productos y llama a `POST /api/comparisons` con sus IDs
5. El sistema valida compatibilidad de categorías, resuelve atributos (reales + virtuales), calcula highlights y retorna la tabla de comparación
6. Si solo interesan las diferencias, se usa `POST /api/comparisons/diff`

---

## Base de datos H2

En desarrollo, la consola de H2 está habilitada:

| Campo | Valor |
|-------|-------|
| URL | [http://localhost:8080/h2-console](http://localhost:8080/h2-console) |
| JDBC URL | `jdbc:h2:mem:meli-challenge-db` |
| Usuario | `sa` |
| Contraseña | *(vacía)* |

---

## Documentación adicional

| Documento | Contenido |
|-----------|-----------|
| [DESIGN.md](docs/DESIGN.md) | Decisiones de diseño y trade-offs del modelo de datos y la lógica de comparación |
| [OBSERVABILITY.md](docs/OBSERVABILITY.md) | Estado actual y plan de observabilidad (Actuator, métricas, logs, alertas) |
| [AVAILABILITY.md](docs/AVAILABILITY.md) | Análisis de disponibilidad |
| [STANDARDS.md](docs/STANDARDS.md) | Convenciones de naming, códigos HTTP y estructura de errores |
| [DB_OPTIMIZATION.md](docs/DB_OPTIMIZATION.md) | Optimizaciones a nivel base de datos