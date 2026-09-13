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

    private final PrinterSettingService printerSettingService;

    @Override
    public void print(byte[] document) {
        PrinterSettingResponse settings = printerSettingService.get();
        String printerName = settings.printerName();

        Path tempFile;
        try {
            tempFile = Files.createTempFile("miresta-ticket-", ".bin");
            Files.write(tempFile, document);
        } catch (IOException e) {
            throw new PrinterException("No se pudo preparar el ticket para imprimir", e);
        }

        try {
            // Un intento más los reintentos configurados — una impresora térmica USB por
            // cola de Windows a veces tiene un hipo puntual (spooler ocupado, cable que
            // se movió) que un segundo intento resuelve solo, sin que el mesero tenga
            // que ir a Admin a reimprimir a mano.
            PrinterException lastError = null;
            for (int attempt = 0; attempt <= settings.retryCount(); attempt++) {
                try {
                    sendOnce(tempFile, printerName, settings.timeoutSeconds());
                    return;
                } catch (PrinterException e) {
                    lastError = e;
                }
            }
            throw lastError;
        } finally {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ignored) {
                // best-effort cleanup of the temp file
            }
        }
    }

    private void sendOnce(Path tempFile, String printerName, int timeoutSeconds) {
        try {
            Process process = new ProcessBuilder(
                    "cmd.exe", "/c", "copy", "/b", tempFile.toString(), "\\\\localhost\\" + printerName)
                    .redirectErrorStream(true)
                    .start();

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished || process.exitValue() != 0) {
                throw new PrinterException("No se pudo enviar el ticket a la impresora \"" + printerName + "\"");
            }
        } catch (IOException e) {
            throw new PrinterException("No se pudo conectar a la impresora \"" + printerName + "\"", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PrinterException("Impresión interrumpida", e);
        }
    }
}
