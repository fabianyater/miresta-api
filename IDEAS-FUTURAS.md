# Ideas para más adelante (sin fecha, sin comprometerse a nada todavía)

Lluvia de ideas del 30/08/2026 — quedan aquí anotadas para retomar cuando se decida
cuál atacar primero. Ninguna se ha diseñado en detalle todavía.

---

## 1. Stock en tiempo real en la pantalla de Tomar Pedido

Hoy "Quedan N" se actualiza solo cuando el mesero refresca/vuelve a consultar (hay un
`refetchInterval` de 15s, pero no es instantáneo entre dispositivos). La idea: que
cuando un producto se agota (o baja de cierto cupo), **todos los celulares conectados
lo vean casi al instante**, sin esperar el próximo refetch — para que dos meseros no
terminen ofreciendo/vendiendo lo mismo que ya se acabó.

Esto probablemente necesita algo tipo WebSocket/Server-Sent Events en vez de solo
polling — es la base técnica de la que dependen las ideas 3 y 4 de abajo (avisos en
tiempo real).

## 2. Enviar el menú del día a los clientes por WhatsApp

Mandar el menú de hoy (probablemente el de Almuerzo) a una lista de clientes por
WhatsApp — para que decidan qué pedir antes de llamar/llegar. Necesita integrarse
con la API de WhatsApp Business (Meta) o un proveedor tipo Twilio/similar.

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

## 4. Avisos en tiempo real de lo que se está agotando

Relacionado con la idea 1: alguna notificación (dentro de la app, o incluso push) que
avise cuando un producto del menú de hoy está por agotarse o ya se agotó, para que
cocina/meseros reaccionen a tiempo.

## 5. Mensajes rápidos de sala a cocina

Un canal simple de mensajitos cortos desde la app hacia cocina — ej. "necesito un
huevo frito", sin tener que ir caminando o gritar. Podría ser tan simple como una
lista de mensajes predefinidos + uno libre, mostrados en una pantalla/tablet en
cocina, o notificaciones push si cocina también tiene un dispositivo.

## 6. Apertura y cierre de caja (turno)

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

**Nota:** las ideas 1, 3 y 4 comparten la misma base técnica (algo en tiempo real —
WebSockets o similar) — probablemente convenga resolver esa base una sola vez y
montar las tres encima, en vez de resolver cada una por separado.

---

## Opinión / orden sugerido (30/08/2026)

1. **#5 (mensajes a cocina)** — primero. Es la más simple, no depende de IA ni de
   WhatsApp, y el valor es inmediato. Ni siquiera necesita tiempo real "de verdad";
   con una lista + polling corto ya resuelve el problema.
2. **#1 (stock en tiempo real)** — segundo, pero empezar simple: bajar el
   `refetchInterval` actual (15s → algo menor) + actualización optimista cuando el
   propio dispositivo hace el cambio, antes de meterse a WebSockets/SSE. Solo
   escalar a algo "de verdad tiempo real" si eso no alcanza.
3. **#4 (avisos de agotados)** — casi gratis una vez resuelto el #1, misma base.
4. **#2 (menú por WhatsApp)** — valor claro, pero la API de WhatsApp Business de
   Meta requiere aprobación de negocio y plantillas pre-aprobadas para mensajes que
   el negocio inicia; conviene arrancar ese trámite temprano aunque el código venga
   después, porque la parte administrativa suele tardar más que la técnica.
5. **#3 (IA + WhatsApp pedidos automáticos)** — de última, y con una salvedad: dado
   lo ambiguo que puede ser interpretar pedidos en español natural (lo vimos incluso
   en esta sesión con el "principio mixto"), la IA **no debería crear el pedido
   directamente** — que arme una propuesta y un mesero/admin la confirme antes de
   que se vuelva un pedido real. Full-auto desde el día uno invita a errores de
   cobro/inventario costosos.
