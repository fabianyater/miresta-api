package com.miresta.shared;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Empuja un aviso liviano ("algo del stock cambió") a todos los dispositivos
 * conectados vía SSE — no manda el dato en sí, cada cliente refresca sus propias
 * consultas al recibirlo. Así un mesero ve casi al instante cuando otro dispositivo
 * agota un producto, sin esperar el próximo polling.
 */
@Component
public class StockEventBroadcaster {

    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));

        try {
            // Sin esto, Tomcat no confirma los headers de la respuesta hasta el primer
            // dato real — el cliente (EventSource) se queda "conectando" indefinidamente
            // en un turno tranquilo. Mandar algo de una vez fuerza el flush inmediato.
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException e) {
            emitters.remove(emitter);
        }

        return emitter;
    }

    public void notifyStockChanged() {
        for (SseEmitter emitter : emitters) {
            send(emitter, SseEmitter.event().name("stock-changed").data("ok"));
        }
    }

    // Sin esto, una conexión que se queda callada mucho rato (nada cambia en el
    // stock) puede morir en silencio a mitad de camino — el proxy de Vite, o
    // cualquier proxy/red intermedio, puede cerrar un socket que lleva minutos sin
    // bytes — y el navegador no siempre nota que ya no está conectado. Un comentario
    // SSE cada 20s no dispara ningún evento en el cliente, solo mantiene el socket
    // con tráfico real para que nadie lo dé por muerto.
    @Scheduled(fixedRate = 20_000)
    public void heartbeat() {
        for (SseEmitter emitter : emitters) {
            send(emitter, SseEmitter.event().comment("keep-alive"));
        }
    }

    // notifyStockChanged() corre en el hilo que atiende la petición que cambió el
    // stock; heartbeat() corre en el hilo del scheduler — sin sincronizar por
    // emitter, ambos podían escribirle al mismo SseEmitter al mismo tiempo (send()
    // no es thread-safe), corrompiendo ese envío puntual y dejando esa conexión
    // sorda desde ahí sin que el cliente se entere.
    private void send(SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        synchronized (emitter) {
            try {
                emitter.send(event);
            } catch (IOException | IllegalStateException e) {
                emitters.remove(emitter);
            }
        }
    }
}
