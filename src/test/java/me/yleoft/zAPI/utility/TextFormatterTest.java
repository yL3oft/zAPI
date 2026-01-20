package me.yleoft.zAPI.utility;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class TextFormatterTest {

    @Nested
    @DisplayName("startsWithIgnoreCase")
    class StartsWithIgnoreCaseTests {

        @ParameterizedTest
        @CsvSource({
                "Hello World, Hello, true",
                "Hello World, HELLO, true",
                "Hello World, hello, true",
                "Hello World, HeLLo, true",
                "Hello World, World, false",
                "Hello, Hello World, false",
                "Test, Test, true",
                "TEST, test, true",
                "'', '', true"
        })
        void shouldMatchCorrectly(String full, String prefix, boolean expected) {
            assertEquals(expected, TextFormatter.startsWithIgnoreCase(full, prefix));
        }

        @Test
        void shouldReturnFalseWhenPrefixLongerThanFull() {
            assertFalse(TextFormatter.startsWithIgnoreCase("Hi", "Hello"));
        }

        @Test
        void shouldHandleEmptyPrefix() {
            assertTrue(TextFormatter.startsWithIgnoreCase("Hello", ""));
        }
    }

    @Nested
    @DisplayName("isInteger")
    class IsIntegerTests {

        @ParameterizedTest
        @ValueSource(strings = {"0", "1", "-1", "123", "-456", "2147483647", "-2147483648"})
        void shouldReturnTrueForValidIntegers(String input) {
            assertTrue(TextFormatter.isInteger(input));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "abc", "12.34", "12a", "a12", " 12", "12 ", "2147483648", "-2147483649"})
        void shouldReturnFalseForInvalidIntegers(String input) {
            assertFalse(TextFormatter.isInteger(input));
        }
    }

    @Nested
    @DisplayName("parseAsTime")
    class ParseAsTimeTests {

        @Test
        void shouldParseSeconds() {
            assertEquals(1000L, TextFormatter.parseAsTime("1s"));
            assertEquals(30000L, TextFormatter.parseAsTime("30s"));
        }

        @Test
        void shouldParseMinutes() {
            assertEquals(60000L, TextFormatter.parseAsTime("1m"));
            assertEquals(300000L, TextFormatter.parseAsTime("5m"));
        }

        @Test
        void shouldParseHours() {
            assertEquals(3600000L, TextFormatter.parseAsTime("1h"));
            assertEquals(7200000L, TextFormatter.parseAsTime("2h"));
        }

        @Test
        void shouldParseDays() {
            assertEquals(86400000L, TextFormatter.parseAsTime("1d"));
            assertEquals(259200000L, TextFormatter.parseAsTime("3d"));
        }

        @Test
        void shouldParseWeeks() {
            assertEquals(604800000L, TextFormatter.parseAsTime("1w"));
            assertEquals(1209600000L, TextFormatter.parseAsTime("2w"));
        }

        @Test
        void shouldParseMonths() {
            assertEquals(2592000000L, TextFormatter.parseAsTime("1mo"));
            assertEquals(7776000000L, TextFormatter.parseAsTime("3mo"));
        }

        @Test
        void shouldParseYears() {
            assertEquals(31536000000L, TextFormatter.parseAsTime("1y"));
            assertEquals(63072000000L, TextFormatter.parseAsTime("2y"));
        }

        @Test
        void shouldParseCombinedTimeStrings() {
            // 1h30m = 3600000 + 1800000 = 5400000
            assertEquals(5400000L, TextFormatter.parseAsTime("1h30m"));

            // 1d12h = 86400000 + 43200000 = 129600000
            assertEquals(129600000L, TextFormatter.parseAsTime("1d12h"));

            // 1y2mo3w4d5h6m7s
            long expected = 31536000000L + 5184000000L + 1814400000L + 345600000L + 18000000L + 360000L + 7000L;
            assertEquals(expected, TextFormatter.parseAsTime("1y2mo3w4d5h6m7s"));
        }

        @Test
        void shouldBeCaseInsensitive() {
            assertEquals(TextFormatter.parseAsTime("1h"), TextFormatter.parseAsTime("1H"));
            assertEquals(TextFormatter.parseAsTime("1mo"), TextFormatter.parseAsTime("1MO"));
            assertEquals(TextFormatter.parseAsTime("1d2h3m"), TextFormatter.parseAsTime("1D2H3M"));
        }

        @Test
        void shouldReturnZeroEmptyString() {
            assertEquals(0L, TextFormatter.parseAsTime(""));
        }

        @Test
        void shouldReturnZeroForNull() {
            assertEquals(0L, TextFormatter.parseAsTime(null));
        }

        @Test
        void shouldReturnZeroForInvalidFormat() {
            // No valid time units found, returns 0
            assertEquals(0L, TextFormatter.parseAsTime("abc"));
            assertEquals(0L, TextFormatter.parseAsTime("invalid"));
        }
    }

    @Nested
    @DisplayName("parseAsString")
    class ParseAsStringTests {

        @Test
        void shouldReturnZeroSecondsForZeroOrNegative() {
            assertEquals("0s", TextFormatter.parseAsString(0));
            assertEquals("0s", TextFormatter.parseAsString(-100));
        }

        @Test
        void shouldFormatSeconds() {
            assertEquals("1s", TextFormatter.parseAsString(1000L));
            assertEquals("30s", TextFormatter.parseAsString(30000L));
            assertEquals("59s", TextFormatter.parseAsString(59000L));
        }

        @Test
        void shouldFormatMinutes() {
            assertEquals("1m", TextFormatter.parseAsString(60000L));
            assertEquals("5m", TextFormatter.parseAsString(300000L));
            assertEquals("5m30s", TextFormatter.parseAsString(330000L));
        }

        @Test
        void shouldFormatHours() {
            assertEquals("1h", TextFormatter.parseAsString(3600000L));
            assertEquals("2h30m", TextFormatter.parseAsString(9000000L));
            assertEquals("1h1m1s", TextFormatter.parseAsString(3661000L));
        }

        @Test
        void shouldFormatDays() {
            assertEquals("1d", TextFormatter.parseAsString(86400000L));
            assertEquals("1d12h", TextFormatter.parseAsString(129600000L));
        }

        @Test
        void shouldFormatWeeks() {
            assertEquals("1w", TextFormatter.parseAsString(604800000L));
            assertEquals("2w3d", TextFormatter.parseAsString(1468800000L));
        }

        @Test
        void shouldFormatMonths() {
            assertEquals("1mo", TextFormatter.parseAsString(2592000000L));
            assertEquals("2mo1w", TextFormatter.parseAsString(5788800000L));
        }

        @Test
        void shouldFormatYears() {
            assertEquals("1y", TextFormatter.parseAsString(31536000000L));
            assertEquals("2y", TextFormatter.parseAsString(63072000000L));
        }

        @Test
        void shouldFormatComplexTimeStrings() {
            // 1y2mo3w4d5h6m7s
            long input = 31536000000L + 5184000000L + 1814400000L + 345600000L + 18000000L + 360000L + 7000L;
            assertEquals("1y2mo3w4d5h6m7s", TextFormatter.parseAsString(input));
        }

        @Test
        void shouldOmitZeroUnits() {
            // 1h and 30s, no minutes
            assertEquals("1h30s", TextFormatter.parseAsString(3630000L));

            // 1d and 5m, no hours
            assertEquals("1d5m", TextFormatter.parseAsString(86700000L));
        }
    }

    @Nested
    @DisplayName("parseAsTime and parseAsString roundtrip")
    class RoundtripTests {

        @ParameterizedTest
        @ValueSource(strings = {"1s", "1m", "1h", "1d", "1w", "1mo", "1y", "1h30m", "1d12h30m15s", "2y3mo1w"})
        void shouldRoundtripCorrectly(String timeString) {
            long ms = TextFormatter.parseAsTime(timeString);
            String result = TextFormatter.parseAsString(ms);
            assertEquals(timeString, result);
        }

        @ParameterizedTest
        @ValueSource(longs = {1000, 60000, 3600000, 86400000, 604800000, 2592000000L, 31536000000L, 5400000, 129600000})
        void shouldRoundtripFromMilliseconds(long ms) {
            String timeString = TextFormatter.parseAsString(ms);
            long result = TextFormatter.parseAsTime(timeString);
            assertEquals(ms, result);
        }
    }
}