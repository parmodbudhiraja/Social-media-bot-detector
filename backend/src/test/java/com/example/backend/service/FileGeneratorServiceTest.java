package com.example.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FileGeneratorServiceTest {

    private FileGeneratorService fileGeneratorService;

    @BeforeEach
    void setUp() {
        fileGeneratorService = new FileGeneratorService();
    }

    @Test
    void testGenerateCsvSuccess() {
        String jsonResult = "{\n" +
                "  \"usernames\": [\"user,1\", \"user2\"],\n" +
                "  \"x_predictions\": [1, 0],\n" +
                "  \"y_predictions\": [1, 0]\n" +
                "}";

        byte[] csvBytes = fileGeneratorService.generateCsv(jsonResult, "all");
        String csv = new String(csvBytes);
        
        assertTrue(csv.contains("Username"));
        assertTrue(csv.contains("\"user,1\"")); // Tests comma escaping
        assertTrue(csv.contains("user2"));
    }

    @Test
    void testGenerateCsvEmptyOrMalformedJson() {
        String jsonResult = "{}";
        byte[] csvBytes = fileGeneratorService.generateCsv(jsonResult, "all");
        String csv = new String(csvBytes);
        
        assertTrue(csv.contains("Username")); // Header still generated
        assertEquals(1, csv.split("\n").length); // Only header row
    }

    @Test
    void testGenerateCsvFilters() {
        String jsonResult = "{\n" +
                "  \"usernames\": [\"bot_user\", \"real_user\"],\n" +
                "  \"x_predictions\": [1, 0],\n" +
                "  \"y_predictions\": [1, 0]\n" +
                "}";

        byte[] csvBytesBots = fileGeneratorService.generateCsv(jsonResult, "bots");
        String csvBots = new String(csvBytesBots);
        assertTrue(csvBots.contains("bot_user"));
        assertFalse(csvBots.contains("real_user"));

        byte[] csvBytesReal = fileGeneratorService.generateCsv(jsonResult, "real");
        String csvReal = new String(csvBytesReal);
        assertTrue(csvReal.contains("real_user"));
        assertFalse(csvReal.contains("bot_user"));
    }
}
