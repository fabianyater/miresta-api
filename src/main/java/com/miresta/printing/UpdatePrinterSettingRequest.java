package com.miresta.printing;

public record UpdatePrinterSettingRequest(String printerName, boolean printingEnabled) {
}
