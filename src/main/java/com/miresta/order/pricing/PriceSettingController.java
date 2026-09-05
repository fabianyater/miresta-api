package com.miresta.order.pricing;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/price-settings")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','OWNER')")
public class PriceSettingController {

    private final PriceSettingService priceSettingService;

    @GetMapping
    public ResponseEntity<List<PriceSettingResponse>> getAll() {
        return ResponseEntity.ok(priceSettingService.getAll());
    }

    @PutMapping("/{code}")
    public ResponseEntity<PriceSettingResponse> update(
            @PathVariable PriceCode code, @RequestBody UpdatePriceSettingRequest request, Authentication authentication) {
        return ResponseEntity.ok(priceSettingService.update(code, request, authentication.getName()));
    }

    @GetMapping("/{code}/history")
    public ResponseEntity<List<PriceSettingHistoryResponse>> history(@PathVariable PriceCode code) {
        return ResponseEntity.ok(priceSettingService.getHistory(code));
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<Void> delete(@PathVariable PriceCode code) {
        priceSettingService.delete(code);
        return ResponseEntity.noContent().build();
    }
}
