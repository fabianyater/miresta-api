# Ideas para más adelante (sin fecha, sin comprometerse a nada todavía)

Lluvia de ideas del 30/08/2026 — quedan aquí anotadas para retomar cuando se decida
cuál atacar primero.

**Estado (14/09/2026):** #1, #2, #4, #5 y #6 ya están implementadas. Solo queda #3
(la más grande, de última a propósito).

---

## 1. Stock en tiempo real en la pantalla de Tomar Pedido — ✅ Hecho

Implementado con Server-Sent Events: `StockEventBroadcaster`/`StockEventController`
en el backend emiten un aviso "stock-changed" (sin datos, solo la señal) cada vez
que se consume/repone algo — sea por un pedido, por editar el menú del día, o por
lotes de producto. El frontend (`useStockEvents`, montado en `AppLayout`) escucha
ese stream y refresca `menus`/`products-stock` casi al instante en todos los
dispositivos conectados, en vez de esperar el `refetchInterval` de 15s (que sigue
ahí como respaldo si el stream se cae). Detalle no obvio: `SseEmitter` usa el
redispatch async del servlet, y el `SecurityContext` por defecto (ThreadLocal) no
cruza a ese segundo hilo — hubo que cambiar la estrategia global a
`MODE_INHERITABLETHREADLOCAL` (`MirestaApplication`) para que no lo rechazara.

## 2. Enviar el menú del día a los clientes por WhatsApp — ✅ Hecho

Implementado como link "click to chat" (`wa.me`) en vez de la API oficial de Meta
Business: en Clientes se seleccionan destinatarios y se abre WhatsApp con el menú
de almuerzo de hoy ya redactado (`lib/whatsapp.ts` + `ClientesPage.tsx`). No manda
el mensaje automáticamente (eso sigue requiriendo la API de Meta) — el mesero/admin
igual tiene que darle enviar en WhatsApp.

## 3. Que un cliente pida por WhatsApp y el pedido entre solo al sistema

Un cliente escribe algo como "hola, un almuerzo completo con pollo, sin sopa" por
WhatsApp, y eso se convierte automáticamente en un pedido real dentro de Miresta —
sin que un mesero lo tenga que digitar. Implica:
- Recibir/leer los mensajes de WhatsApp (webhook de la API de WhatsApp Business).
- Usar IA para interpretar el texto libre del cliente y convertirlo en la estructura
  real de un pedido (qué productos, qué categoría, reemplazos, para llevar o no,
  etc.) — este es el paso donde probablemente se necesite un modelo de lenguaje.
- Antes de armar el pedido (o la IA al interpretar), habría que **saber el stock
  disponible en tiempo real** (idea 1) para no prometerle al cliente algo que ya se
  acabó.
- Crear el pedido en el sistema (reusando `POST /api/orders` o algo similar) y
  responderle al cliente confirmando.

Es la idea más grande de todas — probablemente se divide en varias fases (primero
recibir y mostrarle el pedido interpretado a un mesero para que lo confirme antes de
crearlo de una, en vez de crearlo 100% automático desde el día uno).

## 4. Avisos en tiempo real de lo que se está agotando — ✅ Hecho

Salió gratis con la #1: `StockAlert.tsx` (en Mesas y Pedidos) ya existía y lee la
misma consulta de menús que ahora se refresca al instante vía SSE, así que el aviso
de "Agotado"/"Quedan pocos" ya llega casi en tiempo real sin tocarlo.

## 5. Mensajes rápidos de sala a cocina — ✅ Hecho

Un canal simple de mensajitos cortos desde la app hacia cocina — ej. "necesito un
huevo frito", sin tener que ir caminando o gritar. Podría ser tan simple como una
lista de mensajes predefinidos + uno libre, mostrados en una pantalla/tablet en
cocina, o notificaciones push si cocina también tiene un dispositivo.

## 6. Apertura y cierre de caja (turno) — ✅ Hecho

Manejar el turno de caja como en un POS de verdad:
- **Apertura:** al empezar el día, registrar el fondo/base de caja con el que se
  arranca (efectivo inicial), quién abre y a qué hora.
- **Durante el turno:** entradas/salidas de efectivo que no son ventas (compras de
  cocina pagadas de caja, retiros, etc.).
- **Cierre:** al terminar, el sistema muestra cuánto **debería** haber en efectivo
  (base + ventas en efectivo del turno − salidas), el cajero cuenta lo real y se
  registra el **descuadre** (sobrante/faltante), quién cierra y a qué hora.
- Reporte de arqueo por turno/día, y que el "Resumen del día" y los totales por
  método de pago se aten al turno de caja abierto, no solo a la fecha.

Anotado el 10/09/2026 a pedido de Fabián.

---

**Nota (histórica):** las ideas 1, 3 y 4 compartían la misma base técnica (algo en
tiempo real) — la #1 se resolvió con SSE y la #4 vino gratis con ella. La #3 todavía
necesitaría su propia integración (webhook de WhatsApp), aunque puede reusar el
mismo stream de eventos si en algún momento hace falta avisarle al mesero en vivo.

---

## Opinión / orden sugerido (30/08/2026 — ya resuelto salvo #3)

1. ~~**#5 (mensajes a cocina)**~~ — hecho.
2. ~~**#1 (stock en tiempo real)**~~ — hecho, vía SSE.
3. ~~**#4 (avisos de agotados)**~~ — hecho, gratis con el #1.
4. ~~**#2 (menú por WhatsApp)**~~ — hecho, vía wa.me (sin la API oficial de Meta).
5. **#3 (IA + WhatsApp pedidos automáticos)** — única pendiente, de última a
   propósito y con una salvedad: dado lo ambiguo que puede ser interpretar pedidos en
   español natural (lo vimos incluso en esta sesión con el "principio mixto"), la IA
   **no debería crear el pedido directamente** — que arme una propuesta y un
   mesero/admin la confirme antes de que se vuelva un pedido real. Full-auto desde
   el día uno invita a errores de cobro/inventario costosos.
