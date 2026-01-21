package me.yleoft.zAPI.configuration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PathTest {

    @Test
    void testFormPath_noArgsReturnsEmpty() {
        String result = Path.formPath();
        assertEquals("", result, "formPath() with no args should return empty string");
    }

    @Test
    void testFormPath_singleSegment() {
        String result = Path.formPath("single");
        assertEquals("single", result, "Single segment should be returned as-is");
    }

    @Test
    void testFormPath_multipleSegments() {
        String result = Path.formPath("one", "two", "three");
        assertEquals("one.two.three", result, "Segments should be joined with dots");
    }

    @Test
    void testFormPath_handlesEmptySegments() {
        String result = Path.formPath("", "a", "", "b", "");
        assertEquals("a.b", result, "Empty segments should be ignored");
    }

    @Test
    void testFormPath_ignoresOnlyEmptySegments() {
        String result = Path.formPath("", "");
        assertEquals("", result, "Only-empty segments should produce an empty result");
    }

}
