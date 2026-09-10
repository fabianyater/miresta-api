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
}
