# Meli Challenge API

API REST de consulta y comparación de productos, construida con Spring Boot 3.

El alcance del proyecto está acotado intencionalmente a la consulta y comparación: no expone endpoints de creación ni administración de productos, categorías o atributos. Esta decisión respeta la consigna del challenge, que pone el foco en el motor de comparación. La carga inicial del catálogo se resuelve mediante datos de seed en SQL, lo que permite evaluar el sistema con un conjunto representativo de productos sin necesidad de una capa de ingesta. Se asume que dicha capa de ingesta es responsable de las validaciones, conversiones de unidades y resolución de sinónimos necesarias para que los datos persistan en el formato que el modelo requiere.

El proyecto incluye documentación OpenAPI auto-generada, manejo centralizado de errores con respuestas JSON estandarizadas y un pipeline de tests.

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

### Docker

El proyecto incluye un Dockerfile multi-stage (build con JDK 21, runtime con JRE 21 Alpine):

```bash
# Construir la imagen
docker build -t meli-challenge .

# Ejecutar el contenedor
docker run -p 8080:8080 meli-challenge
```

---

## Tests

```bash
# Ejecutar todos los tests
mvn test

# Ejecutar tests con reporte de cobertura (JaCoCo)
mvn verify
# Reporte HTML en: target/site/jacoco/index.html
```

El proyecto tiene seis niveles de tests:

- **Unitarios de servicio** — lógica de negocio aislada con Mockito, sin contexto Spring
  - `CategoryServiceTest` — árbol, detalle, atributos y categorías comparables
  - `ProductServiceTest` — búsqueda, listado por categoría y detalle de producto
  - `ComparisonServiceTest` — comparación completa: misma categoría, cross-categoría, validaciones y casos de error
  - `CurrencyExchangeServiceTest` — normalización de tipo de cambio a ARS, caché y fallback si la API key está ausente
  - `ProductFieldResolverTest` — resolución de atributos virtuales: precio normalizado, descuento, envío gratis y rating
- **Unitarios de mapper** — conversión entidad → DTO sin dependencias externas
  - `CategoryMapperTest` — mapeo a `CategoryTreeDTO` (recursivo) y `CategorySummaryDTO`
  - `ProductMapperTest` — mapeo a `ProductSummaryDTO` con precio, envío y valores nulos
- **Unitarios de repositorio** — queries JPQL/nativas contra H2 en memoria (`@DataJpaTest`)
  - `RepositoryTest` — búsqueda de productos por nombre/marca/modelo, reglas de atributos por categoría, pares de categorías comparables
- **De manejo de excepciones** — respuestas de error estandarizadas
  - `GlobalExceptionHandlerTest` — verifica status HTTP y cuerpo `ApiError` para `ItemNotFoundException` (404), `CategoryMismatchException` (422), `InvalidComparisonRequestException` (400) y violaciones de constraint
- **De controller** (`@WebMvcTest` + MockMvc)
  - `CategoryControllerTest`, `ProductControllerTest`, `ComparisonControllerTest`
- **De integración** — stack completo contra H2 con `data.sql` real
  - `ApiIntegrationTest` (`@SpringBootTest`)

---

## Frontend Demo

Se creó un frontend simple utilizando la herramienta de IA [v0.dev](https://v0.dev) para visualizar los datos devueltos por esta API. Podés encontrar el código fuente y las instrucciones en el siguiente repositorio:

🔗 https://github.com/santiagojpaez/v0-product-comparison-app

Para ejecutar el frontend localmente:

```bash
git clone https://github.com/santiagojpaez/v0-product-comparison-app.git
cd v0-product-comparison-app
npm install --legacy-peer-deps
npm run dev
```

Asegurate de tener el sistema corriendo en [http://localhost:8080/](http://localhost:8080/) para que la demostración funcione

---

## Documentación OpenAPI

Con el servidor corriendo:

| Recurso | URL |
|---------|-----|
| Swagger UI (interactivo) | [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html) |
| OpenAPI spec (JSON) | [http://localhost:8080/api-docs](http://localhost:8080/api-docs) |

El spec cubre todos los endpoints, parámetros, DTOs de request/response y códigos de error.

Además, contiene una descripción del funcionamiento de cada endpoint con especificaciones detalladas de los parámetros de entrada, esquemas de respuesta y la funcionalidad 'Try it out', que permite ejecutar ejemplos predefinidos como casos de prueba.

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
| [DB_OPTIMIZATION.md](docs/DB_OPTIMIZATION.md) | Optimizaciones a nivel base de datos |
| [IMPROVEMENTS.md](docs/IMPROVEMENTS.md) | Mejoras a realizar ante salida a producción |
| [DATA_MODEL.md](docs/DATA_MODEL.md) | Descripción detallada del modelo de datos |
| [AI.md](docs/AI.md) | Utilización de Inteligencia Artificial durante el desarrollo