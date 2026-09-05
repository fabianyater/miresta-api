# Brief de proyecto — Miresta (POS restaurante familiar)

> Documento pensado para pegarse como contexto completo en una conversación nueva (con Claude, con otro asistente, o para un desarrollador que se sume) sin perder las decisiones ya tomadas. Autocontenido: no depende de conversaciones previas.

## 1. Qué es el negocio

Restaurante familiar (nombre real pendiente de confirmar — **"Sazón de Mamá" es un placeholder**, no el nombre definitivo). Vende diariamente:

- **Desayunos**
- **Almuerzos**
- Los **domingos**, además del almuerzo regular: **almuerzo especial** (sancocho)

El menú cambia cada día, aunque ciertos días se repite — de ahí la idea de **plantillas de menú reutilizables** en vez de cargar el menú de cero cada vez (esto ya se refleja parcialmente en las entidades `Menu` / `MenuService` actuales, ver §4).

## 2. Objetivo central de este proyecto

**Los precios deben ser administrables desde la app.** El dueño necesita poder cambiar un precio base (por ejemplo, el valor del "completo" de almuerzo) y que todo lo que depende de ese valor — recargos por adición, reglas de combo — recalcule solo, sin tocar código ni desplegar.

Esto es la restricción de diseño más importante para el modelo de dominio: **los precios y las reglas de combo no pueden quedar hardcodeados**. El motor de precios necesita ser datos configurables (una tabla de precios / reglas), no constantes en el código.

El proyecto actual "funciona a medias" — la lógica de precios existente no cubre completamente las reglas descritas abajo y hay que revisarla contra el modelo real.

## 3. Motor de precios

Todos los precios están en pesos (COP), sin decimales.

### 3.1 Desayuno

| Pedido | En sitio | Para llevar |
|---|---|---|
| Completo (caldo + bandeja: arroz, arepa, patacona, proteína) | 8.000 | 9.000 |
| Solo bandeja (sin caldo) | 7.000 | 8.000 |
| Solo caldo | 6.000 | 7.000 |
| Adición de acompañamiento | 1.000 | igual |
| Adición de proteína | 4.000 | igual |
| Huevo adicional — como acompañante | 1.000 | igual |
| Huevo adicional — como otra proteína (edge case raro) | 4.000 | igual |

**Regla de bandeja completa:** proteína + al menos 2 de los 3 acompañamientos (arroz, arepa, patacona) = precio de bandeja completo, sin importar cuál de los 3 falte.

Pedidos más reducidos que eso (ej. solo arepa + proteína, solo arroz + proteína) se cobran por componentes sueltos. **Precios PROVISIONALES** (recuperados de un proyecto anterior, pendientes de confirmar con el negocio):
- Proteína sola: 4.000
- Sopa/caldo suelto: 6.000
- Componente suelto: 1.000

### 3.2 Almuerzo

| Pedido | En sitio | Para llevar |
|---|---|---|
| Completo (sopa + bandeja: principio, arroz, ensalada, maduro, proteína) | 10.000 | 12.000 |
| Solo bandeja (sin sopa) | 9.000 | 10.000 |
| Solo sopa | 6.000 | 6.000 (igual, no sube) |
| Adición de acompañamiento | 1.000 | igual |
| Adición de proteína | 5.000 | igual |
| Huevo adicional — como acompañante | 1.000 | igual |
| Huevo adicional — como otra proteína (edge case) | 5.000 | igual |

**Regla de bandeja completa:** proteína + al menos 2 de los 4 acompañamientos (principio, arroz, ensalada, maduro) = precio de bandeja completo.

Reducido (menos que eso) se cobra por partes. **Precios PROVISIONALES** salvo el componente suelto, que ya queda confirmado:
- Proteína sola: 4.000
- Sopa suelta: 5.000
- Componente suelto: **1.000, confirmado — aplica por igual a cualquiera de los 4 acompañamientos** (principio, arroz, ensalada, maduro), igual que la fila "Adición de acompañamiento" de la tabla de arriba. La restricción del proyecto anterior (solo ensalada y arroz sueltos) **no aplica** — cada acompañamiento suelto vale 1.000 sin excepción.

### 3.3 Almuerzo especial — domingo (sancocho)

Es casi un plato fijo — precio único casi sin importar composición.

| Pedido | En sitio | Para llevar |
|---|---|---|
| Completo (sancocho + bandeja) | 25.000 | 25.000 (igual) |
| Solo bandeja (sin sancocho) | 25.000 | 25.000 |
| Adición de acompañamiento | 1.000 | igual |
| Adición de proteína | 12.000 | igual |

Puede ir sin arroz y sin ensalada — sigue valiendo lo mismo. Componente suelto en un pedido muy mínimo: **10.000 (PROVISIONAL, caso raro)**.

### 3.4 Regla transversal: el huevo es un rol, no un producto

El huevo no es un producto especial con su propio precio fijo — **el precio depende de qué rol ocupa dentro del pedido**, igual que cualquier otro componente podría ocuparlo.

| Situación | Efecto en el precio |
|---|---|
| Reemplaza la proteína | Precio normal de proteína, sin cobro especial |
| Reemplaza el principio o el arroz (no ensalada, no maduro) | Sin cobro extra |
| Reemplaza la sopa/caldo | El pedido sigue contando como "completo", sin cobro extra |
| Adición pura como acompañante | 1.000 (desayuno) / 1.000 (almuerzo) |
| Adición ocupando el rol de "otra proteína" (ya tiene su proteína normal y pide huevo de más *como si fuera* otra porción de proteína, no un acompañante) | Cuesta como adición de proteína: 4.000 (desayuno) / 5.000 (almuerzo) |

Esta noción de "rol" (proteína / acompañamiento / sopa, y qué producto concreto lo ocupa) es probablemente el concepto de dominio más importante para modelar bien el motor de precios. **Corrección: sí está modelado hoy** — son las `Category` del catálogo (`sopa`, `principios`, `proteinas`, `acompanantes`, `adicionales`, `bebidas`, `especiales`, `envase`), y `OrderItemSelection.replacementForCategory` es exactamente el campo que declara qué categoría/rol está ocupando una selección cuando reemplaza a otra. Ver el detalle real de la implementación en §5.

## 4. Decisiones de arquitectura ya tomadas

(Del diagnóstico previo del modelo de dominio — ver mapa completo con ER y hallazgos en el artifact publicado: https://claude.ai/code/artifact/61590473-2e38-43e5-94bf-98733f9c4525)

- **Mantener arquitectura en capas**, pero **reorganizar por dominio/feature** (`order`, `menu`, `table`, `catalog`) en vez de por tipo técnico (`controller`/`service`/`repository`/`entity` planos). **No** ir a hexagonal completo por ahora — no hay evidencia de múltiples adaptadores reales que lo justifiquen.
- Renombrar la entidad `MenuService` → algo como `MenuOffering` o `MenuSitting`, para eliminar el choque de nombre con la capa de servicios (`IMenuServicesService` / `MenuServicesServiceImpl`).
- Repositorios duplicados detectados, pendientes de fusionar: `DiningRepository` vs `TableEntityRepository` (ambos sobre `DiningTable`, cada uno usado por un service distinto); `DiningTableStatusRepository` vs `TableStatusEntityRepository` (el segundo sin ningún uso).
- Entidades huérfanas sin conectar al resto del grafo de dominio: `Customer` (POJO plano, ni siquiera `@Entity`) y `PaymentType` (sí es `@Entity` pero `Order` no tiene relación hacia él) — pendiente decidir si se completan o se eliminan.
- `OrderType` está a nivel de `OrderItem`, no de `Order` — parece intencional: existe `isTogoPrice` por ítem, lo que sugiere que un mismo pedido puede mezclar ítems para comer en el local y para llevar. Pendiente confirmarlo como regla de negocio explícita.

## 5. Estado actual del motor de precios en código

El motor ya existe y es más completo de lo que parecía a primera vista — vive sobre todo en `OrderItemServiceImpl.updateTotalPrice()`, con una segunda copia parcial de la tabla de precios en `OrderItemSelectionsImpl.resolveUnitExtraPrice()`. El concepto de "rol" (§3.4) sí está modelado: `Category` del catálogo (`sopa`, `principios`, `proteinas`, `acompanantes`, `adicionales`, `bebidas`, `especiales`, `envase`) + `OrderItemSelection.replacementForCategory` para declarar qué categoría reemplaza una selección. La lógica ya cubre combos completo/bandeja, créditos por sopa/principio/acompañante/proteína, y la exención del huevo cuando reemplaza el principio.

**Huecos reales frente a este brief** (confirmados leyendo el código, no suposición):

- **Nada de esto es administrable todavía.** Todos los precios son constantes Java (`BASE_FULL_PRICE_LUNCH`, `TOGO_PRICE`, el switch de `individualsUnitPrice()`, etc.), duplicadas además en dos clases distintas que hay que mantener sincronizadas a mano. Este es el bloqueo principal para el objetivo de §2.
- `TOGO_PRICE` es un flat de 1.000 aplicado a cualquier ítem con `orderType = OUT`, incluyendo los que se cobran por componentes individuales (que según §3 no deberían subir de precio para llevar). Además no diferencia desayuno (+1.000) de almuerzo (+2.000 según §3.2) — hoy siempre suma 1.000.
- **No existe rama de precio para `ALMUERZO ESPECIAL` (domingo).** El switch de `mealType` solo maneja `ALMUERZO` y `DESAYUNO`; un pedido de domingo cae al camino de "individuales" y nunca recibe el precio fijo de 25.000.
- `principios` (principio) se cuenta en un bucket separado (`principleCredits`) de `acompanantes` (`sideCredits`), pero ese `principleCredits` **no se usa** en la condición `hasBasicCombo` de almuerzo — es decir, hoy el principio no cuenta para completar el umbral "2 de 4 acompañamientos" aunque el negocio lo considera uno de los 4.
- Hay un mecanismo de reemplazo a nivel de catálogo (`Product.getActsAsCategory()`, leído por reflection en `getActsAsCategorySafe`) que no existe como campo en la entidad `Product` — siempre devuelve `null`. Es código aspiracional/muerto, no una regla activa.

## 6. Pendientes / preguntas para el negocio

- Confirmar todos los precios marcados **PROVISIONAL** en la tabla `price_settings` (`confirmed = false`) — vienen de un proyecto anterior, no del negocio actual. Editables desde `PUT /api/v1/price-settings/{code}`.
- Confirmar nombre real del restaurante (reemplazar placeholder "Sazón de Mamá").
- Ajustar las mesas placeholder (`dining_table`, sembradas como 1–6) a la cantidad real del salón.
- Conseguir/config­urar la impresora POS de red y apuntar `PrinterSetting.ip` a su IP real (hoy placeholder `192.168.1.50:9100`).

## 7. Estado tras la implementación (26 ago 2026)

Todo lo de §2–§6 fue implementado en esta sesión — reorganización por dominio, motor de precios administrable, clientes frecuentes, tipos de pago + reporte de caja, autenticación propia con roles, e impresión térmica. Detalle completo y decisiones tomadas durante la implementación en el historial de la conversación; lo operativo:

**Arquitectura final** — paquetes por dominio: `catalog`, `menu`, `table`, `order` (+ `order.pricing`), `customer`, `auth`, `printing`, `shared` (value objects `Money` y `ComboCategory`). La entidad `MenuService` quedó renombrada a `MenuOffering` (tabla incluida). Repos duplicados fusionados, código muerto (`Customer` como POJO plano, reflection hacks) eliminado.

**Esquema de BD** — migrado de `ddl-auto: update` a **Flyway** (`src/main/resources/db/migration`, V1–V7). La BD local se recreó desde cero para adoptar Flyway limpio (confirmado como descartable).

**Autenticación** — JWT propio (ya no Keycloak). Login: `POST /api/auth/login`. Usuario admin sembrado: `admin@miresta.local` / `MirestaAdmin2026!` — **cambiar esta contraseña después del primer login** (no hay endpoint de cambio de password todavía, se actualiza vía `PATCH /api/v1/users/{id}` recreando el hash, o directo en BD mientras se agrega esa función). Roles: `ADMIN` (todo) y `MESERO` (menú en solo lectura, mesas, pedidos, clientes, tipos de pago, imprimir comanda/cuenta).

**Motor de precios** — todos los montos viven en `price_settings`, editables vía `PriceSettingController`. `PricingCalculator` es la única fuente de la lógica de combos/roles (antes duplicada). Bugs corregidos de paso: el recargo para llevar ya no se duplicaba con el precio combo to-go (bug nuevo detectado y corregido durante la reescritura), la adición de proteína ahora varía por comida (antes 4.000 fijo sin importar desayuno/almuerzo/especial), y un doble cobro en adicionales con cantidad > 1.

**Verificado end-to-end**: `./gradlew test` (suite de `PricingCalculatorTest` + `MirestaApplicationTests` con Spring context completo) y, contra Postgres real, un flujo completo por API (crear productos → crear menú → crear pedido → ver total calculado → cobrar → ver reporte de caja): almuerzo completo en sitio (10.000 ✓), bandeja para llevar con principio contando para el umbral (10.000 ✓, sin duplicar el recargo to-go), huevo reemplazando principio sin cobro extra (✓), especial de domingo a precio fijo (✓), reporte de pagos agregando por método (✓).

**Bug encontrado y corregido durante esta verificación**: cualquier excepción no controlada en el backend se devolvía como `403 Forbidden` en vez de su código real — la falla de conexión a la impresora placeholder (esperada, no hay impresora física) se disfrazaba de "prohibido" en vez de un error de servidor. Se agregó `ApiExceptionHandler` (`com.miresta.shared`) con manejo explícito: `EntityNotFoundException`→404, `EntityExistsException`→409, `AccessDeniedException`→403 (el real), `PrinterException`→502, resto→400/500. Verificado que los 403 legítimos (rol sin permiso) siguen siendo 403 y que los demás casos ahora muestran su código real.

**No verificado**: impresión real (no hay impresora física a mano) — el mecanismo de transporte (`EscPosNetworkTicketPrinter`) está listo para apuntar a una IP real apenas se compre/conecte la impresora; hoy falla limpiamente con 502 y un mensaje claro, como se espera sin hardware.
