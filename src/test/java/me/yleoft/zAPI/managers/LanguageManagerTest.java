package me.yleoft.zAPI.managers;

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static me.yleoft.zAPI.utils.ConfigUtils.formPath;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class LanguageManagerTest {

    public static LanguageManager langm;

    public static String cmds = "commands";

    public interface DefaultEnum {
        default String defparser(@Nullable String result, String... replacements) {
            if(result == null) result = this.toString();
            for (int i = 0; i < replacements.length; i += 2) {
                String placeholder = replacements[i];
                String value = (i + 1 < replacements.length) ? replacements[i + 1] : "";
                result = result.replace(placeholder, value);
            }
            return result;
        }
        default String defparse(String... replacements) {
            return defparser(null, replacements);
        }

        static String defpath() {
            return formPath();
        }
    }
    public interface CommandsEnum extends DefaultEnum {
        default String parse(String... replacements) {
            return defparse(replacements);
        }

        static String defpath() {
            return formPath(DefaultEnum.defpath(), cmds);
        }
    }

    @BeforeAll
    static void setup() throws Exception {
        langm = new LanguageManager(
                new File(LanguageManagerTest.class.getClassLoader()
                        .getResource("languages").getFile()),
                "pt-br",
                "en"
        );
    }

    @Test
    void testFolderLoaded() {
        assertNotNull(langm.getFolder(), "Folder should be loaded");
    }

    @Test
    void testLanguageLoaded() {
        assertEquals("pt-br", langm.getCurrentLanguage(), "Current language should be pt-br");
        assertEquals("en", langm.getFallbackLanguage(), "Fallback language should be en");
    }

    @Test
    void testMessages() {
        assertEquals("[pt-br] Esta é uma configuração de teste", Messages.TEST_CFG.toString());
        assertEquals("[pt-br] Esta é uma configuração de teste em bloco\n" +
                "Pode abranger várias linhas\n" +
                "E preservar a formatação\n", Messages.TEST_CFGBLOCK.toString());
        assertEquals("[pt-br] Esta é uma configuração de teste com uma variável: variable", Messages.TEST_CFGVAR.defparse("%test%", "variable"));
        assertEquals("[pt-br] Esta é uma mensagem de teste apenas no arquivo de idioma português do Brasil", Messages.TEST_ONLY_PTBR.toString());
        assertEquals("[en] This is a test message only in the English language file", Messages.TEST_ONLY_US.toString());
    }

    @Test
    void testCommands() {
        assertEquals("[pt-br] /testcmd", TestCMD.USAGE.toString());
        assertEquals("[pt-br] Esta é uma saída de comando de teste para /testcmd", TestCMD.OUTPUT.toString());
    }

    public enum Messages implements DefaultEnum {
        TEST_CFG("test-cfg"),
        TEST_CFGBLOCK("test-cfgblock"),
        TEST_CFGVAR("test-cfgvar"),
        TEST_ONLY_US("test-only-us"),
        TEST_ONLY_PTBR("test-only-pt-br"),
        ;

        private final String path;
        Messages(String path) {
            this.path = path;
        }

        @Override
        public String toString() {
            return langm.getString(formPath(DefaultEnum.defpath(), path));
        }
    }

    public enum TestCMD implements CommandsEnum {
        USAGE("usage"),
        OUTPUT("output"),
        ;

        private final String path;
        TestCMD(String path) {
            this.path = path;
        }

        public static String path() {
            return formPath(CommandsEnum.defpath(), "testcmd");
        }

        private static String parseCMD(String text) {
            return text.replace("%command%", "testcmd");
        }

        @Override
        public String toString() {
            return parseCMD(langm.getString(formPath(path(), path)));
        }
    }

}