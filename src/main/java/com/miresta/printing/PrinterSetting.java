package com.miresta.printing;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Single-row configuration for the POS printer. Editable from the app so the
 * Windows printer name can change (new printer, reinstalled driver) without a redeploy.
 */
@Getter
@Setter
@Entity
@Table(name = "printer_setting")
public class PrinterSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "printer_name", nullable = false)
    private String printerName;

    /** Cuando está en false, los tickets no se envían de verdad a la impresora — solo
     * se arma la vista previa. Útil en desarrollo sin impresora conectada, o si la
     * impresora real está fuera de servicio y no se quiere bloquear la toma de pedidos. */
    @Column(name = "printing_enabled", nullable = false)
    private boolean printingEnabled = true;

    /** Nombre/marca del negocio — primera línea del encabezado de cada ticket. */
    @Column(name = "header_line_1", nullable = false)
    private String headerLine1;

    /** Segunda línea del encabezado (ej. eslogan) — opcional, no se imprime si es null/vacía. */
    @Column(name = "header_line_2")
    private String headerLine2;

    /** Dirección/NIT u otra línea informativa — opcional. */
    @Column(name = "address_line")
    private String addressLine;

    /** Mensaje de despedida en el recibo de pago — opcional. */
    @Column(name = "footer_message")
    private String footerMessage;

    /** Ancho útil en caracteres — 32 para papel de 58mm, 48 para 80mm. */
    @Column(name = "paper_width_chars", nullable = false)
    private int paperWidthChars = 32;

    /** Si la impresora no tiene cuchilla, se puede apagar y solo se alimenta papel. */
    @Column(name = "auto_cut", nullable = false)
    private boolean autoCut = true;

    /** Reintentos automáticos si el envío falla (además del primer intento). */
    @Column(name = "retry_count", nullable = false)
    private int retryCount = 2;

    /** Cuánto esperar a que la cola de Windows termine de recibir el ticket antes de darlo por fallido. */
    @Column(name = "timeout_seconds", nullable = false)
    private int timeoutSeconds = 10;
}
