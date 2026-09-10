package com.miresta.printing;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal ESC/POS command builder — just enough to print the three ticket types this
 * POS needs (comanda, cuenta, resumen del día). No external thermal-printer library:
 * these are well-known, stable byte sequences.
 *
 * Alongside the raw byte stream, it keeps a parallel structured line list (text +
 * bold/center/rule flags) — the only way to also render a preview of the same ticket
 * (used when there's no real printer to send it to) without reverse-parsing ESC/POS
 * bytes.
 */
public class EscPosDocument {

    private static final Charset PRINTER_CHARSET = Charset.forName("ISO-8859-1");

    public record Line(String text, boolean bold, boolean center, boolean rule) {
    }

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final List<Line> lines = new ArrayList<>();

    private boolean boldOn = false;
    private boolean centerOn = false;

    public EscPosDocument() {
        out.writeBytes(new byte[]{0x1B, 0x40}); // ESC @ — initialize
    }

    public EscPosDocument center() {
        centerOn = true;
        out.writeBytes(new byte[]{0x1B, 0x61, 0x01});
        return this;
    }

    public EscPosDocument left() {
        centerOn = false;
        out.writeBytes(new byte[]{0x1B, 0x61, 0x00});
        return this;
    }

    public EscPosDocument bold(boolean on) {
        boldOn = on;
        out.writeBytes(new byte[]{0x1B, 0x45, (byte) (on ? 1 : 0)});
        return this;
    }

    public EscPosDocument line(String text) {
        out.writeBytes(text.getBytes(PRINTER_CHARSET));
        out.writeBytes(new byte[]{0x0A});
        lines.add(new Line(text, boldOn, centerOn, false));
        return this;
    }

    public EscPosDocument blankLine() {
        return line("");
    }

    public EscPosDocument rule() {
        line("--------------------------------");
        lines.set(lines.size() - 1, new Line("", boldOn, centerOn, true));
        return this;
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

    /** The structured lines behind this document, for a preview — same content, sin los
     * bytes de control de la impresora real. */
    public List<Line> lines() {
        return List.copyOf(lines);
    }
}
