package me.yleoft.zAPI.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class StringUtilsTest {

    @Test
    void testHex_singleReplacement() {
        String input = "Hello &#A1B2C3World";
        String expected = "Hello &x&A&1&B&2&C&3World";
        assertEquals(expected, StringUtils.hex(input));
    }

    @Test
    void testHex_multipleReplacements() {
        String input = "Start &#112233 middle &#AABBCC end";
        String expected = "Start &x&1&1&2&2&3&3 middle &x&A&A&B&B&C&C end";
        assertEquals(expected, StringUtils.hex(input));
    }

    @Test
    void testIsInteger_trueForIntegers_falseForOthers() {
        assertTrue(StringUtils.isInteger("0"));
        assertTrue(StringUtils.isInteger("12345"));
        assertTrue(StringUtils.isInteger("-7"));
        assertFalse(StringUtils.isInteger("1.5"));
        assertFalse(StringUtils.isInteger("abc"));
        assertFalse(StringUtils.isInteger("12abc"));
    }

    @Test
    void testParseAsTime_and_parseAsString_roundtrip_hour_minute() {
        String input = "1h30m";
        long ms = StringUtils.parseAsTime(input);
        assertEquals(3_600_000L + 1_800_000L, ms);
        String formatted = StringUtils.parseAsString(ms);
        assertEquals("1h30m", formatted);
    }

    @Test
    void testParseAsString_zero_and_negative() {
        assertEquals("0s", StringUtils.parseAsString(0));
        assertEquals("0s", StringUtils.parseAsString(-100));
    }

    @Test
    void testParseAsTime_complex_roundtrip() {
        String input = "2h15m30s";
        long ms = StringUtils.parseAsTime(input);
        assertEquals(7_200_000L + 900_000L + 30000L, ms);
        String formatted = StringUtils.parseAsString(ms);
        assertTrue(formatted.startsWith("2h15m"));
    }

    @Test
    void parseAsString_combinesUnitsCorrectly() {
        long ms = 1L * 24 * 60 * 60 * 1000     // day
                + 2L * 60 * 60 * 1000          // hours
                + 3L * 60 * 1000               // minutes
                + 4L * 1000;                   // seconds
        String out = StringUtils.parseAsString(ms);
        // Expect to see day, hours and minutes, and trailing seconds '4s'
        assertTrue(out.contains("d"), "Should contain days");
        assertTrue(out.contains("h"), "Should contain hours");
        assertTrue(out.contains("m"), "Should contain minutes");
        assertTrue(out.endsWith("4s"), "Should end with seconds '4s'");
    }
}