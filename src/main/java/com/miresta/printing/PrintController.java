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
    public ResponseEntity<Void> printComanda(@PathVariable Long orderId) {
        ticketService.printComanda(orderId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/orders/{orderId}/print/cuenta")
    @PreAuthorize("hasAnyRole('ADMIN','MESERO','OWNER')")
    public ResponseEntity<Void> printCuenta(@PathVariable Long orderId) {
        ticketService.printCuenta(orderId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/reports/print/resumen")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<Void> printResumen(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        ticketService.printResumenDelDia(date);
        return ResponseEntity.ok().build();
    }
}
