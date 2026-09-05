package com.miresta.printing;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Sends the raw ESC/POS bytes to the printer through its Windows-shared local queue,
 * using the classic "copy /b file \\localhost\share" trick: the spooler forwards the
 * bytes to the port untouched (works with the "Generic / Text Only" driver these
 * cheap USB thermal printers use), no native binding needed. If the printer ends up
 * reachable over the network instead, only this class needs to change — nothing else
 * in the printing module depends on the transport.
 */
@RequiredArgsConstructor
@Service
public class EscPosUsbTicketPrinter implements TicketPrinter {

    private static final long COPY_TIMEOUT_SECONDS = 10;

    private final PrinterSettingService printerSettingService;

    @Override
    public void print(byte[] document) {
        String printerName = printerSettingService.get().printerName();

        Path tempFile;
        try {
            tempFile = Files.createTempFile("miresta-ticket-", ".bin");
            Files.write(tempFile, document);
        } catch (IOException e) {
            throw new PrinterException("No se pudo preparar el ticket para imprimir", e);
        }

        try {
            Process process = new ProcessBuilder(
                    "cmd.exe", "/c", "copy", "/b", tempFile.toString(), "\\\\localhost\\" + printerName)
                    .redirectErrorStream(true)
                    .start();

            boolean finished = process.waitFor(COPY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished || process.exitValue() != 0) {
                throw new PrinterException("No se pudo enviar el ticket a la impresora \"" + printerName + "\"");
            }
        } catch (IOException e) {
            throw new PrinterException("No se pudo conectar a la impresora \"" + printerName + "\"", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PrinterException("Impresión interrumpida", e);
        } finally {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ignored) {
                // best-effort cleanup of the temp file
            }
        }
    }
}
