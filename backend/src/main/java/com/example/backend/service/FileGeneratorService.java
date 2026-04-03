package com.example.backend.service;

import org.springframework.stereotype.Service;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class FileGeneratorService {

    public void generateCategorizedFiles(String jobId, Map<TruthTableService.CategorizationResult, List<String>> categorizedUsers) {
        String baseDir = System.getProperty("java.io.tmpdir") + "/v1/" + jobId;
        File dir = new File(baseDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        try {
            writeListToFile(baseDir + "/certainly_fake.txt", categorizedUsers.get(TruthTableService.CategorizationResult.CERTAINLY_FAKE));
            writeListToFile(baseDir + "/may_be_fake.txt", categorizedUsers.get(TruthTableService.CategorizationResult.MAY_BE_FAKE));
            writeListToFile(baseDir + "/may_be_real.txt", categorizedUsers.get(TruthTableService.CategorizationResult.MAY_BE_REAL));
            writeListToFile(baseDir + "/certainly_real.txt", categorizedUsers.get(TruthTableService.CategorizationResult.CERTAINLY_REAL));
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate files for job " + jobId, e);
        }
    }

    private void writeListToFile(String path, List<String> usernames) throws IOException {
        try (FileWriter writer = new FileWriter(path)) {
            if (usernames != null) {
                for (String username : usernames) {
                    writer.write(username + System.lineSeparator());
                }
            }
        }
    }
}
