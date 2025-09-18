package me.yleoft.zAPI.utils;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

public class SkullUtilsTest {

    public static String url = "https://textures.minecraft.net/texture/82c2ec1bab8f723ec509a77bbb8ed8ac64ce1c612e631e28611549dc470ee3e2";

    @Test
    void decodeSkinUrl_validBase64ReturnsUrl() {
        String json = "{\"textures\":{\"SKIN\":{\"url\":\""+url+"\"}}}";
        String base64 = Base64.getEncoder().encodeToString(json.getBytes());
        String result = SkullUtils.decodeSkinUrl(base64);
        assertEquals(url, result);
    }

    @Test
    void decodeSkinUrl_missingTexturesReturnsNull() {
        String json = "{\"no_textures\":{}}";
        String base64 = Base64.getEncoder().encodeToString(json.getBytes());
        assertNull(SkullUtils.decodeSkinUrl(base64));
    }

    @Test
    void decodeSkinUrl_missingSkinReturnsNull() {
        String json = "{\"textures\":{}}";
        String base64 = Base64.getEncoder().encodeToString(json.getBytes());
        assertNull(SkullUtils.decodeSkinUrl(base64));
    }

    @Test
    void decodeSkinUrl_emptyStringReturnsNull() {
        assertNull(SkullUtils.decodeSkinUrl(""));
    }
}