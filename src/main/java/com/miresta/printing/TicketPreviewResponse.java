package com.miresta.printing;

import java.util.List;

/** printed=false means the ticket was built but NOT actually sent to a real printer
 * (impresión desactivada en Admin > Impresora) — the frontend shows the lines as a
 * preview in that case instead of just a "listo, se imprimió" toast. */
public record TicketPreviewResponse(String title, boolean printed, List<TicketLineResponse> lines) {
}
