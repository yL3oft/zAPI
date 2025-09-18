package me.yleoft.zAPI.handlers;

import me.yleoft.zAPI.zAPI;
import org.bukkit.OfflinePlayer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PlaceholderAPIHandlerTest {

    @AfterEach
    void teardown() {
        zAPI.setPlaceholderAPIHandler(new PlaceholderAPIHandler());
    }

    @Test
    void applyPlaceholders_returnsOriginal_whenPlayerNull() {
        PlaceholderAPIHandler handler = new PlaceholderAPIHandler();
        String input = "This has %placeholders_blah% but player is null";
        assertEquals(input, handler.applyPlaceholders(null, input));
    }

    @Test
    void applyPlaceholders_returnsOriginal_whenNoPercentSign() {
        PlaceholderAPIHandler handler = new PlaceholderAPIHandler();
        OfflinePlayer fake = null;
        String input = "plain text without placeholders";
        assertEquals(input, handler.applyPlaceholders(fake, input));
    }

}