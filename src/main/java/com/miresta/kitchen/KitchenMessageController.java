package com.miresta.kitchen;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/kitchen/messages")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MESERO', 'ADMIN', 'OWNER')")
public class KitchenMessageController {

    private final KitchenMessageService service;

    @PostMapping
    public ResponseEntity<KitchenMessageResponse> send(
            @RequestBody KitchenMessageRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.send(request.text(), authentication.getName()));
    }

    @GetMapping
    public ResponseEntity<List<KitchenMessageResponse>> list(
            @RequestParam(value = "since", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant since) {
        return ResponseEntity.ok(service.list(since));
    }
}
