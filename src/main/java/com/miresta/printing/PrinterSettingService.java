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
        PrinterSetting setting = current();
        return new PrinterSettingResponse(setting.getPrinterName());
    }

    @Transactional
    public PrinterSettingResponse update(UpdatePrinterSettingRequest request) {
        PrinterSetting setting = current();
        setting.setPrinterName(request.printerName());
        printerSettingRepository.save(setting);
        return new PrinterSettingResponse(setting.getPrinterName());
    }

    private PrinterSetting current() {
        return printerSettingRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Printer setting not seeded"));
    }
}
