package me.yleoft.zAPI;

/**
 * Test-only stub for LanguageManager.
 */
public final class zAPI {

    private static final TestLogger LOGGER = new TestLogger();

    private zAPI() {}

    public static TestLogger getLogger() {
        return LOGGER;
    }

    public static final class TestLogger {
        public void warn(String message) {
            // swallow in tests (or store messages if you want to assert them)
        }
    }
}