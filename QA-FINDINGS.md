# QA — Pedidos, precios y menú (30/08/2026)

> **Estado:** los 4 casos están arreglados y reverificados en vivo el mismo día.

Revisión de QA enfocada en el flujo de pedidos (creación, precios, mesas, menú del
día, clientes). Cada caso fue **reproducido en vivo** contra el backend real (no es
solo lectura de código) y limpiado después de confirmarlo. Ordenados de más a menos
grave.

---

## Caso 1 — [ARREGLADO] Un plato de la categoría "Especiales" pedido solo cobra $12.000 en vez de $25.000

**Afecta:** Bandeja paisa, Sancocho de gallina, Churrasco, Costilla de cerdo al horno
en BBQ, Mojarra frita, Arroz con pollo — cualquier producto de la categoría
`ESPECIAL` del catálogo.

**Cómo reproducirlo:**
1. Tomar Pedido → Especial.
2. Pedir *solo* "Bandeja paisa" (nada de sopa ni proteína aparte).
3. Enviar.

**Esperado:** $25.000 (precio fijo `ESPECIAL_COMPLETO`), igual que cualquier plato
especial.

**Actual (confirmado en vivo):** $12.000 — el precio de "componente suelto"
(`ESPECIAL_COMPONENTE_SUELTO`), como si fuera un acompañamiento cobrado por separado.

**Causa técnica:** en `PricingCalculator.priceOrderItem`, la categoría `ESPECIAL` no
suma nada a `totalProteins` ni a `effectiveSoupCredits` (cae al `default -> {}` del
switch). La regla que decide si se arma el combo de Especial es
`totalProteins >= 1 || effectiveSoupCredits >= 1` — como un producto `ESPECIAL` no
aporta a ninguno de los dos, **nunca** forma el combo por sí solo, y cae al precio
individual/suelto. El único test existente para Especial completo
(`especialDomingo_precioFijo_sinImportarBandejaOCompleto`) prueba Sancocho(SOPA) +
Pollo(PROTEINA) — nunca prueba un producto `ESPECIAL` solo, por eso no se había
detectado.

**Impacto:** cualquier venta de un plato especial "de una sola pieza" (que es
probablemente el caso más común para estos platos) se está cobrando $13.000 por
debajo del precio real.

**Arreglo aplicado:** un producto de categoría `ESPECIAL` ahora cuenta por sí solo
para armar el combo (ya no depende de que también haya sopa/proteína aparte), y cada
unidad pedida es su propio plato completo — 2x Bandeja paisa cobra $50.000, no
$25.000. De paso se simplificó la pantalla de Tomar Pedido: cuando el tipo de comida
es "Especial", solo se muestran las categorías **Especiales** y **Bebidas** (ya no
aparecen Sopas/Principios/Proteínas/Acompañantes, que no aplican a un plato especial
de una sola pieza) — el cliente, "para llevar" y las notas siguen igual.
Reverificado en vivo: 1x Bandeja paisa = $25.000, 2x = $50.000.

---

## Caso 2 — [ARREGLADO] Dos pedidos casi simultáneos a la misma mesa crean dos tickets separados en vez de uno

**Cómo se reprodujo:** se dispararon dos `POST /api/orders` para la misma mesa al
mismo tiempo (simulando dos meseros, o el mismo mesero con mala señal tocando
"Enviar" dos veces desde dos dispositivos). Resultado real:

```
"id":15, diningTable:{"id":8,"number":6}, total:1000
"id":16, diningTable:{"id":8,"number":6}, total:1000
```

Dos pedidos `PENDING` distintos para la mesa 6, en vez de uno solo con ambos platos.

**Esperado:** el segundo pedido debería sumarse al mismo ticket pendiente de la mesa
(que es justamente el comportamiento normal cuando las peticiones no llegan exactamente
al mismo tiempo).

**Causa técnica:** en `OrderServiceImpl.createOrder`, la mesa se revisa así:

```java
Optional<Order> existingOrder = orderRepository.findByDiningTable_IdAndDiningTable_Status_NameAndOrderStatus_Name(...);
if (existingOrder.isPresent()) { ... } else { crear uno nuevo }
```

Es un patrón "leer, después decidir, después escribir" sin ningún bloqueo. Si dos
transacciones llegan a la vez, ambas pueden ver "no hay pedido todavía" antes de que
la otra confirme el suyo, y las dos terminan creando un pedido nuevo.

**Impacto:** con varios meseros usando el celular al mismo tiempo (que es justamente
el escenario que se dejó funcionando esta sesión), esto puede pasar de verdad — la
pantalla "Ya hay un pedido en esta mesa" en Tomar Pedido solo muestra uno de los dos
tickets, así que el otro queda ahí sin que nadie se dé cuenta hasta que se revisa
Pedidos o Reportes.

**¿Es un problema de idempotencia?** No exactamente, aunque están relacionados.
Idempotencia es "repetir la MISMA petición no debería tener un efecto distinto a
hacerla una vez" (típicamente con una llave que el cliente genera y el servidor usa
para detectar un reintento) — eso sirve para el caso de "se me colgó y reenvié lo
mismo". Aquí el problema es distinto: son **dos peticiones genuinamente distintas**
(dos meseros añadiendo cosas reales a la misma mesa) que compiten por la misma
decisión ("¿ya existe un pedido pendiente para esta mesa?") sin ningún bloqueo — es
una **condición de carrera**, se arregla con control de concurrencia (un bloqueo a
nivel de base de datos al leer/crear el pedido de la mesa, o una restricción única
que impida dos pedidos `PENDING` para la misma mesa a la vez), no con una llave de
idempotencia.

**Arreglo aplicado:** al crear un pedido para una mesa, la fila de la mesa se lee con
un bloqueo (`SELECT ... FOR UPDATE`, vía `findByIdForUpdate`) antes de revisar si ya
hay un pedido pendiente. Así, si dos peticiones llegan casi al mismo tiempo para la
misma mesa, la segunda espera a que la primera termine (cree o reutilice el pedido)
antes de hacer su propia revisión — y por lo tanto ya encuentra y reutiliza lo que la
primera acaba de crear, en vez de crear otro aparte. Mesas distintas no se bloquean
entre sí, solo compite quien toca la misma mesa a la vez.

Reverificado en vivo con el mismo experimento (dos `POST /api/orders` disparados en
paralelo a la misma mesa, cada uno con un plato distinto): antes daba dos pedidos
`PENDING` separados; ahora da **un solo pedido con los dos platos**.

---

## Caso 3 — [ARREGLADO] Editar el menú del día resetea el stock ya vendido

**Cómo se reprodujo:**
1. Menú del día → Almuerzo → Cerdo a la plancha con cupo de 5.
2. Se piden 3 (quedan 2, confirmado por API).
3. Se vuelve a guardar el mismo menú desde Admin (por ejemplo, para agregar un
   producto nuevo o corregir algo — **sin tocarle nada a Cerdo a la plancha**).
4. Cerdo a la plancha vuelve a mostrar **5 disponibles**, como si nadie hubiera
   pedido nada.

**Esperado:** editar el menú (agregar/quitar productos, ajustar cupos de *otros*
productos) no debería afectar el conteo de lo que ya se vendió de los productos que
se dejan igual.

**Causa técnica:** `MenuItemServiceImpl.replaceMenuItems` borra **todas** las filas
de `menu_item` de esa oferta y las vuelve a crear desde cero con las cantidades que
vengan en el formulario — no hay ningún ajuste relativo, es un reemplazo total. Como
la pantalla de edición del Menú del día (`MenuPage.tsx`) guarda con el mismo
`createMenu` tanto para crear como para editar (`isEditing ? 'Menú actualizado' : ...`),
cualquier guardado — aunque sea solo para agregar una bebida — reescribe los cupos
originales de todo lo demás, sin importar cuánto se haya consumido ya.

**Impacto:** es fácil de disparar sin querer (cualquier ajuste al menú a mitad del
día), y el efecto es que la cocina puede terminar vendiendo de más de un producto que
en la práctica ya se había agotado.

**Arreglo aplicado:** `menu_item` ahora guarda por separado el **cupo configurado**
(`initial_quantity`) y **cuánto queda** (`quantity`, la misma columna de siempre).
Al editar el menú, en vez de borrar y recrear todo, se recalcula cuánto queda a
partir del cupo nuevo menos lo que ya se consumió (`nuevo_cupo - ya_consumido`) —
así, resubmitir el mismo cupo deja el consumo intacto, y subir/bajar el cupo de
verdad ajusta lo que queda de forma proporcional. Reverificado en vivo: cupo 5,
consumidos 3 (quedan 2) → reguardar con cupo 5 sigue en 2 (antes volvía a 5); subir
el cupo a 10 → pasa a 7 (10 − 3), como se espera.

---

## Caso 4 — [ARREGLADO] "Hoy" se calculaba en UTC, no en la hora local — todas las noches desde las 7pm la app cree que ya es mañana

**Encontrado de pasada** verificando el Caso 1 en vivo (no estaba en la revisión
original).

**Cómo se reprodujo:** siendo las 7:1x pm hora de Bogotá del 30/08, Tomar Pedido
mostraba "No hay menú de especial configurado para hoy" pese a que el menú de
Especial de hoy (30/08) sí estaba configurado y se veía bien por API directa.

**Causa técnica:** `todayIso()` (duplicada en `TomarPedidoPage.tsx`, `MenuPage.tsx`,
`ReportesPage.tsx` y `PedidosPage.tsx`) usaba `new Date().toISOString().slice(0, 10)`
— `toISOString()` da la fecha en **UTC**, no la fecha local del navegador. Bogotá es
UTC-5, así que desde las 7:00pm hora local en adelante, la fecha UTC ya cayó en el
día siguiente.

**Impacto:** todas las noches, justo en horas de cena/cierre:
- Tomar Pedido busca el menú de *mañana* (que normalmente aún no existe) en vez del
  de hoy.
- Reportes, por defecto, muestra el resumen (vacío) de mañana en vez del cierre de
  hoy.
- El Historial de Pedidos por defecto también apunta a la fecha equivocada.

**Arreglo aplicado:** se centralizó `todayIso()` en `lib/utils.ts` usando la fecha
**local** (`getFullYear`/`getMonth`/`getDate`, no `toISOString`), y se reemplazaron
las 4 copias duplicadas por este único helper. Reverificado en vivo a las 7pm hora
local: el menú de Especial de hoy ya carga bien.

---

## Riesgo relacionado, no confirmado directamente ni arreglado (mismo patrón que tenía el Caso 2)

Cobrar (`updateOrderStatus`) y fiar (`fiarCliente`, `payOrder`) siguen el mismo
patrón de "leer el pedido, decidir, guardar" sin bloqueo que tenía el Caso 2 antes
del arreglo. No se reprodujo en vivo un doble-cobro, pero dos toques casi
simultáneos de "Cobrar" (por ejemplo, un mesero toca dos veces por mala conexión)
podrían en teoría llegar a marcarlo completado dos veces o pisarse el método de
pago. El mismo enfoque del Caso 2 (bloquear la fila del pedido con
`findByIdForUpdate` antes de decidir) serviría acá también — queda pendiente si se
quiere aplicar.

---

## Nota menor (no es bug, solo un dato suelto)

Los códigos de precio `ALMUERZO_COMPONENTE_SUELTO` y `DESAYUNO_COMPONENTE_SUELTO`
existen en la tabla de precios pero **ningún cálculo los usa realmente** (quedaron
huérfanos de una versión anterior de la lógica). Siguen en $0/sin confirmar y no
tienen ningún efecto — no hace falta tocarlos, solo dejarlo anotado por si alguna vez
causa confusión ver esos dos siempre en $0 en la pantalla de Precios.
