package com.miresta.shared;

import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/stock-events")
@RequiredArgsConstructor
public class StockEventController {

    private final StockEventBroadcaster broadcaster;

    // Sin @PreAuthorize específico: cualquier usuario autenticado puede suscribirse,
    // el aviso no lleva datos sensibles, solo dice "algo cambió, vuelve a consultar".
    //
    // La URL de este stream es siempre la misma para una misma sesión (mismo JWT en
    // el query param) — sin "no-cache" explícito, un navegador/proxy podría servir
    // una conexión vieja cacheada en vez de abrir una nueva de verdad al recargar,
    // dejando ese dispositivo sordo para siempre aunque se vea "conectado".
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> subscribe() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(broadcaster.subscribe());
    }
}
