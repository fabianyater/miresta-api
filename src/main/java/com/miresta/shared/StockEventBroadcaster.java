package com.miresta.shared;

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
            try {
                emitter.send(SseEmitter.event().name("stock-changed").data("ok"));
            } catch (IOException | IllegalStateException e) {
                emitters.remove(emitter);
            }
        }
    }
}
