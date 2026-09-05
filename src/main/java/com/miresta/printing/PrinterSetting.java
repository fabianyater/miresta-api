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
}
