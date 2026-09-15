package com.miresta.shared;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
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
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        return broadcaster.subscribe();
    }
}
