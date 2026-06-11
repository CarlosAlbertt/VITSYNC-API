package com.ejemplo.vitsync.util;

/**
 * Minimal free-text sanitizer for stored values that are later rendered in
 * the frontend (chat messages, report notes, profile fields).
 *
 * <p>Strategy: strip HTML tags and neutralise the characters used to break
 * out of an HTML/JS context. This is defence-in-depth against stored XSS
 * (audit finding V16); the frontend must still escape on output. No external
 * library is used to keep the dependency surface small.</p>
 *
 * @author VitSync Team
 * @version 1.0
 * @since 2.0
 */
public final class HtmlSanitizer {

    private HtmlSanitizer() {
    }

    /**
     * Removes HTML tags and escapes the residual angle brackets/quotes.
     *
     * @param input untrusted free text (may be {@code null})
     * @return sanitised text, or {@code null} if input was {@code null}
     */
    public static String sanitize(String input) {
        if (input == null) {
            return null;
        }
        // 1) Elimina cualquier etiqueta <...> completa (incluye <script>)
        String noTags = input.replaceAll("(?is)<[^>]*>", "");
        // 2) Neutraliza brackets/comillas sueltos que quedaran
        return noTags
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;")
                .trim();
    }
}
