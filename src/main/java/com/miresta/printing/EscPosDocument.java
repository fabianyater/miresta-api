package com.miresta.printing;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;

/**
 * Minimal ESC/POS command builder — just enough to print the three ticket types this
 * POS needs (comanda, cuenta, resumen del día). No external thermal-printer library:
 * these are well-known, stable byte sequences.
 */
public class EscPosDocument {

    private static final Charset PRINTER_CHARSET = Charset.forName("ISO-8859-1");

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    public EscPosDocument() {
        out.writeBytes(new byte[]{0x1B, 0x40}); // ESC @ — initialize
    }

    public EscPosDocument center() {
        out.writeBytes(new byte[]{0x1B, 0x61, 0x01});
        return this;
    }

    public EscPosDocument left() {
        out.writeBytes(new byte[]{0x1B, 0x61, 0x00});
        return this;
    }

    public EscPosDocument bold(boolean on) {
        out.writeBytes(new byte[]{0x1B, 0x45, (byte) (on ? 1 : 0)});
        return this;
    }

    public EscPosDocument line(String text) {
        out.writeBytes(text.getBytes(PRINTER_CHARSET));
        out.writeBytes(new byte[]{0x0A});
        return this;
    }

    public EscPosDocument blankLine() {
        return line("");
    }

    public EscPosDocument rule() {
        return line("--------------------------------");
    }

    public EscPosDocument feed(int lines) {
        for (int i = 0; i < lines; i++) {
            out.writeBytes(new byte[]{0x0A});
        }
        return this;
    }

    public EscPosDocument cut() {
        feed(3);
        out.writeBytes(new byte[]{0x1D, 0x56, 0x00}); // GS V 0 — full cut
        return this;
    }

    public byte[] toBytes() {
        return out.toByteArray();
    }
}
