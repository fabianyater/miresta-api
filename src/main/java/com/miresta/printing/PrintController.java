package com.miresta.printing;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
public class PrintController {

    private final TicketService ticketService;

    @PostMapping("/api/orders/{orderId}/print/comanda")
    @PreAuthorize("hasAnyRole('ADMIN','MESERO','OWNER')")
    public ResponseEntity<TicketPreviewResponse> printComanda(@PathVariable Long orderId) {
        return ResponseEntity.ok(ticketService.printComanda(orderId));
    }

    @PostMapping("/api/orders/{orderId}/print/cuenta")
    @PreAuthorize("hasAnyRole('ADMIN','MESERO','OWNER')")
    public ResponseEntity<TicketPreviewResponse> printCuenta(@PathVariable Long orderId) {
        return ResponseEntity.ok(ticketService.printCuenta(orderId));
    }

    @PostMapping("/api/reports/print/resumen")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<TicketPreviewResponse> printResumen(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ticketService.printResumenDelDia(date));
    }
}
