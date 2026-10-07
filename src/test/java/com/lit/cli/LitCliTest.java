package com.lit.cli;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LitCliTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @BeforeEach
    void setUpStreams() {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    void testHelpCommand() {
        LitCli.main(new String[]{"--help"});
        String output = outContent.toString();
        assertTrue(output.contains("Uso: lit <comando> [argumentos]"));
        assertTrue(output.contains("Comandos disponibles:"));
        assertTrue(output.contains("init"));
        assertTrue(output.contains("status"));
        assertTrue(output.contains("save"));
    }

    @Test
    void testHelpCommandAlternative() {
        LitCli.main(new String[]{"help"});
        String output = outContent.toString();
        assertTrue(output.contains("LIT - Light Intelligent Tracking"));
    }
}
