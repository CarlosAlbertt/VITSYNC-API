package com.ejemplo.vitsync.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link HtmlSanitizer}.
 */
@DisplayName("HtmlSanitizer — stored XSS defence")
class HtmlSanitizerTest {

    @Test
    @DisplayName("null returns null")
    void nullReturnsNull() {
        assertNull(HtmlSanitizer.sanitize(null));
    }

    @Test
    @DisplayName("Strips a script tag")
    void stripsScriptTag() {
        String out = HtmlSanitizer.sanitize("hola<script>alert(1)</script>mundo");
        assertFalse(out.contains("<script>"));
        assertFalse(out.toLowerCase().contains("<script"));
        assertTrue(out.contains("hola"));
        assertTrue(out.contains("mundo"));
    }

    @Test
    @DisplayName("Escapes residual angle brackets and quotes")
    void escapesResidual() {
        String out = HtmlSanitizer.sanitize("2 < 3 & \"q\" 'a'");
        assertTrue(out.contains("&lt;"));
        assertTrue(out.contains("&quot;"));
        assertTrue(out.contains("&#x27;"));
    }

    @Test
    @DisplayName("Plain text is preserved (trimmed)")
    void plainTextPreserved() {
        assertEquals("texto normal", HtmlSanitizer.sanitize("  texto normal  "));
    }
}
