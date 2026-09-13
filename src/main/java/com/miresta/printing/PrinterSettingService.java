package com.miresta.printing;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class PrinterSettingService {

    private final PrinterSettingRepository printerSettingRepository;

    public PrinterSettingResponse get() {
        return toResponse(current());
    }

    @Transactional
    public PrinterSettingResponse update(UpdatePrinterSettingRequest request) {
        if (request.headerLine1() == null || request.headerLine1().isBlank()) {
            throw new IllegalArgumentException("La primera línea del encabezado no puede estar vacía.");
        }
        if (request.paperWidthChars() < 20 || request.paperWidthChars() > 64) {
            throw new IllegalArgumentException("El ancho de papel debe estar entre 20 y 64 caracteres.");
        }
        if (request.retryCount() < 0 || request.retryCount() > 5) {
            throw new IllegalArgumentException("Los reintentos deben ser entre 0 y 5.");
        }
        if (request.timeoutSeconds() < 3 || request.timeoutSeconds() > 60) {
            throw new IllegalArgumentException("El tiempo de espera debe ser entre 3 y 60 segundos.");
        }

        PrinterSetting setting = current();
        setting.setPrinterName(request.printerName());
        setting.setPrintingEnabled(request.printingEnabled());
        setting.setHeaderLine1(request.headerLine1().trim());
        setting.setHeaderLine2(blankToNull(request.headerLine2()));
        setting.setAddressLine(blankToNull(request.addressLine()));
        setting.setFooterMessage(blankToNull(request.footerMessage()));
        setting.setPaperWidthChars(request.paperWidthChars());
        setting.setAutoCut(request.autoCut());
        setting.setRetryCount(request.retryCount());
        setting.setTimeoutSeconds(request.timeoutSeconds());
        printerSettingRepository.save(setting);
        return toResponse(setting);
    }

    public boolean isPrintingEnabled() {
        return current().isPrintingEnabled();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private PrinterSettingResponse toResponse(PrinterSetting setting) {
        return new PrinterSettingResponse(
                setting.getPrinterName(),
                setting.isPrintingEnabled(),
                setting.getHeaderLine1(),
                setting.getHeaderLine2(),
                setting.getAddressLine(),
                setting.getFooterMessage(),
                setting.getPaperWidthChars(),
                setting.isAutoCut(),
                setting.getRetryCount(),
                setting.getTimeoutSeconds());
    }

    private PrinterSetting current() {
        return printerSettingRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Printer setting not seeded"));
    }
}
