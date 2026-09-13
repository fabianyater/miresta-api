package com.miresta.kitchen;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kitchen/phrases")
@RequiredArgsConstructor
public class KitchenPhraseController {

    private final KitchenPhraseService service;

    @GetMapping
    @PreAuthorize("@access.has('COCINA_VER')")
    public ResponseEntity<List<String>> list() {
        return ResponseEntity.ok(service.list());
    }

    @PutMapping
    @PreAuthorize("@access.has('COCINA_FRASES_EDITAR')")
    public ResponseEntity<List<String>> replace(@RequestBody KitchenPhrasesRequest request) {
        return ResponseEntity.ok(service.replace(request.phrases()));
    }
}
