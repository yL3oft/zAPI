package me.yleoft.zAPI.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ItemStackUtilsTest {

    @Test
    void legacyColors_containsExpectedEntries() {
        assertTrue(ItemStackUtils.LEGACY_COLORS.containsKey("WHITE"));
        assertEquals(Integer.valueOf(0), ItemStackUtils.LEGACY_COLORS.get("WHITE"));

        assertTrue(ItemStackUtils.LEGACY_COLORS.containsKey("BLACK"));
        assertEquals(Integer.valueOf(15), ItemStackUtils.LEGACY_COLORS.get("BLACK"));

        assertTrue(ItemStackUtils.LEGACY_COLORS.containsKey("RED"));
        assertEquals(Integer.valueOf(14), ItemStackUtils.LEGACY_COLORS.get("RED"));

        assertTrue(ItemStackUtils.LEGACY_COLORS.size() >= 16);
    }
}