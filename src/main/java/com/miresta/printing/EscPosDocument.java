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
 * bold/center/rule/big flags) — the only way to also render a preview of the same
 * ticket (used when there's no real printer to send it to) without reverse-parsing
 * ESC/POS bytes.
 */
public class EscPosDocument {

    private static final Charset PRINTER_CHARSET = Charset.forName("ISO-8859-1");

    /** Ancho útil por defecto en caracteres — impresora térmica de 58mm con la fuente A. */
    public static final int DEFAULT_WIDTH = 32;

    public record Line(String text, boolean bold, boolean center, boolean rule, boolean big) {
    }

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final List<Line> lines = new ArrayList<>();
    private final int width;

    private boolean boldOn = false;
    private boolean centerOn = false;

    public EscPosDocument() {
        this(DEFAULT_WIDTH);
    }

    /** @param width Ancho útil en caracteres — 32 para papel de 58mm, 48 para 80mm. */
    public EscPosDocument(int width) {
        this.width = width;
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
        lines.add(new Line(text, boldOn, centerOn, false, false));
        return this;
    }

    /**
     * Línea de título — texto a doble alto y ancho en la impresora real, y marcada
     * como {@code big} para que la vista previa la muestre más grande.
     */
    public EscPosDocument title(String text) {
        out.writeBytes(new byte[]{0x1D, 0x21, 0x11}); // GS ! — double width + height
        out.writeBytes(new byte[]{0x1B, 0x45, 1});    // bold on
        out.writeBytes(text.getBytes(PRINTER_CHARSET));
        out.writeBytes(new byte[]{0x0A});
        out.writeBytes(new byte[]{0x1B, 0x45, (byte) (boldOn ? 1 : 0)});
        out.writeBytes(new byte[]{0x1D, 0x21, 0x00}); // reset size
        lines.add(new Line(text, true, centerOn, false, true));
        return this;
    }

    /**
     * Fila de dos columnas: {@code left} pegado a la izquierda y {@code right} alineado
     * a la derecha, rellenando con espacios hasta el ancho del papel. Si no caben en
     * una línea, se dejan separados por un espacio y la impresora hace el wrap.
     */
    public EscPosDocument row(String left, String right) {
        String l = left == null ? "" : left;
        String r = right == null ? "" : right;
        int gap = width - l.length() - r.length();
        return line(gap > 0 ? l + " ".repeat(gap) + r : l + " " + r);
    }

    public EscPosDocument blankLine() {
        return line("");
    }

    public EscPosDocument rule() {
        line("-".repeat(width));
        lines.set(lines.size() - 1, new Line("", boldOn, centerOn, true, false));
        return this;
    }

    public EscPosDocument feed(int lines) {
        for (int i = 0; i < lines; i++) {
            out.writeBytes(new byte[]{0x0A});
        }
        return this;
    }

    /** Alimenta papel para poder rasgarlo y, si {@code autoCut} está activo, además
     * manda el comando de corte — algunas impresoras (sin cuchilla) no lo soportan. */
    public EscPosDocument cut(boolean autoCut) {
        feed(3);
        if (autoCut) {
            out.writeBytes(new byte[]{0x1D, 0x56, 0x00}); // GS V 0 — full cut
        }
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
