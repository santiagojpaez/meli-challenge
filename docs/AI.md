# Asistencia con IA

## Aclaración

El challenge fue recibido a través de la plataforma HackerRank y solicitaba realizarse en su propio entorno de desarrollo integrado web. Sin embargo, opté deliberadamente por trabajar en mi IDE local por las siguientes razones.

El entorno de HackerRank no dispone de integración con agentes de IA que asistan en tiempo real durante el desarrollo. En mi flujo de trabajo diario, la IA me permite iterar más rápido, explorar alternativas de diseño antes de comprometerse con una, y mantener el foco en las decisiones que realmente importan en lugar de en la mecánica de escribirlas.

Dado el alcance del sistema requerido, completar el challenge en el tiempo asignado sin asistencia de IA hubiera implicado sacrificar profundidad de diseño en favor de velocidad de escritura. Eso hubiera producido exactamente lo contrario al resultado que me gustaría demostrar en el mismo.

Trabajar en mi entorno local con un agente de IA me permitió enfocarlo como lo haría en un proyecto real: discutir decisiones de diseño antes de implementarlas, iterar sobre el modelo de datos, generar documentación técnica de calidad y cubrir casos de prueba no obvios.

## Algunos Prompts Utilizados

### 1. Diseño del modelo entidad-relación

> Diseñar el modelo de entidad-relación para una API de comparación de productos inspirado en cómo lo resuelve Mercado Libre internamente. El modelo debe responder dos preguntas centrales: cómo comparar atributos equivalentes entre productos cuando los vendedores los nombran de forma distinta, y cómo garantizar que productos de determinadas categorías tengan ciertos atributos definidos. Incluir jerarquía de categorías, definición canónica de atributos con sinónimos, normalización de unidades de medida, reglas de atributos por categoría con flags de obligatoriedad, y productos con sus atributos ya normalizados al momento de la carga.

---

### 2. Incorporación de grupos de atributos al modelo

> Extender el modelo para que los atributos puedan asociarse a un grupo de presentación como "Características Principales" o "Cámara". Evaluar si modelarlo como entidad separada con FK desde la regla de categoría o como campos directos, considerando que el mismo atributo puede pertenecer a grupos distintos en categorías distintas.

---

### 3. Documentación OpenAPI / Swagger UI

> Agregar descripciones a cada endpoint en OpenAPI / Swagger UI. Explicar los parámetros de cada uno para que puedan completarlos fácilmente. Incorporar ejemplos (casos de prueba) con sus respuestas esperadas, considerando los datos insertados en la base de datos.

---

### 4. Actualización del README con tests

> Modificar el README incluyendo los nuevos tests incorporados, de manera resumida.

---

### 5. Revisión de documentación completa

> Recorrer toda la documentación (README y archivos dentro de docs/) buscando inconsistencias o posibles mejoras. Las mejoras no consisten en explayarse más o reexplicar lo obvio, sino en agregar cosas no contempladas.

---

### 6. Decisión sobre comparabilidad entre categorías

> Revisar la decisión de derivar comparabilidad entre categorías a partir del árbol jerárquico. Evaluar en qué casos tiene sentido real comparar productos de categorías hermanas y en cuáles no, ilustrándolo con un ejemplo concreto. Proponer una alternativa de modelado más explícita y justificar por qué es preferible.

---

### 7. Revisión de DTOs innecesarios

> Evaluar si el endpoint de validación previa de comparabilidad y su DTO asociado tienen sentido en un sistema donde la UI ya controla qué productos puede seleccionar el usuario. Determinar bajo qué condiciones mantenerlo agrega valor real.

---

### 8. Casos de prueba para QA

> Dado el schema completo con sus datos de seed y la especificación OpenAPI, generar casos de prueba complejos desde la perspectiva de un quality tester profesional. Enfocarse en casos no obvios: comparaciones cross-categoría, atributos virtuales, inconsistencias en los datos de seed, comportamiento del diff cuando todos los valores son iguales, validaciones en los límites e IDs duplicados.
