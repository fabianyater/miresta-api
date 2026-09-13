package com.miresta.printing;

public record UpdatePrinterSettingRequest(
        String printerName,
        boolean printingEnabled,
        String headerLine1,
        String headerLine2,
        String addressLine,
        String footerMessage,
        int paperWidthChars,
        boolean autoCut,
        int retryCount,
        int timeoutSeconds) {
}
