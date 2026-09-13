package com.miresta.printing;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/printer-setting")
@RequiredArgsConstructor
@PreAuthorize("@access.has('IMPRESORA_CONFIG')")
public class PrinterSettingController {

    private final PrinterSettingService printerSettingService;
    private final TicketService ticketService;

    @GetMapping
    public ResponseEntity<PrinterSettingResponse> get() {
        return ResponseEntity.ok(printerSettingService.get());
    }

    @PutMapping
    public ResponseEntity<PrinterSettingResponse> update(@RequestBody UpdatePrinterSettingRequest request) {
        return ResponseEntity.ok(printerSettingService.update(request));
    }

    @PostMapping("/test-print")
    public ResponseEntity<TicketPreviewResponse> testPrint() {
        return ResponseEntity.ok(ticketService.printTestTicket());
    }
}
