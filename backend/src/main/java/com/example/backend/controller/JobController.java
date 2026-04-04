package com.example.backend.controller;

import com.example.backend.model.AnalysisJob;
import com.example.backend.service.ApifyService;
import com.example.backend.service.JobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/jobs")
@CrossOrigin(origins = "*") // For development, allow React frontend CORS
public class JobController {

    private static final Logger logger = LoggerFactory.getLogger(JobController.class);

    private final JobService jobService;
    private final ApifyService apifyService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Autowired
    public JobController(JobService jobService, ApifyService apifyService, com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.jobService = jobService;
        this.apifyService = apifyService;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<AnalysisJob> initiateJob(@RequestBody Map<String, String> payload) {
        String url = payload.get("url");
        logger.info("API: POST /api/v1/jobs - Received request to scan URL: {}", url);
        
        if (url == null || url.trim().isEmpty()) {
            logger.warn("API: POST /api/v1/jobs - Bad Request: URL is missing or empty.");
            return ResponseEntity.badRequest().build();
        }
        
        try {
            AnalysisJob job = jobService.createJob(url);
            logger.debug("API: POST /api/v1/jobs - Created job ID: {}", job.getJobId());
            
            // Trigger the asynchronous Apify scraping workflow
            apifyService.triggerScraping(job);
            
            return ResponseEntity.ok(job);
        } catch (Exception e) {
            logger.error("API: POST /api/v1/jobs - Internal Server Error: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<AnalysisJob> getJobStatus(@PathVariable String jobId) {
        logger.info("API: GET /api/v1/jobs/{} - Status check received", jobId);
        return jobService.getJob(jobId)
                .map(job -> {
                    logger.debug("API: GET /api/v1/jobs/{} - Returning status: {}", jobId, job.getStatus());
                    return ResponseEntity.ok(job);
                })
                .orElseGet(() -> {
                    logger.warn("API: GET /api/v1/jobs/{} - Job not found.", jobId);
                    return ResponseEntity.notFound().build();
                });
    }

    @GetMapping("/{jobId}/download")
    public ResponseEntity<byte[]> downloadJobResults(@PathVariable String jobId) {
        return jobService.getJob(jobId).map(job -> {
            if (job.getResult() == null) {
                return ResponseEntity.noContent().<byte[]>build();
            }

            try {
                com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(job.getResult());
                com.fasterxml.jackson.databind.JsonNode usernames = root.get("usernames");
                com.fasterxml.jackson.databind.JsonNode x_preds = root.get("x_predictions");
                com.fasterxml.jackson.databind.JsonNode y_preds = root.get("y_predictions");

                StringBuilder csv = new StringBuilder();
                csv.append("Username,ProfileAuthenticity,BehavioralAuthenticity,FinalVerdict\n");

                for (int i = 0; i < usernames.size(); i++) {
                    String user = usernames.get(i).asText();
                    int x = x_preds.get(i).asInt();
                    int y = y_preds.get(i).asInt();
                    
                    String verdict = (x == 0 && y == 0) ? "CERT_REAL" : (x == 1 && y == 1) ? "CERT_FAKE" : "PROB_FAKE";
                    csv.append(String.format("%s,%d,%d,%s\n", user, x, y, verdict));
                }

                byte[] bytes = csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
                return ResponseEntity.ok()
                        .header("Content-Type", "text/csv")
                        .header("Content-Disposition", "attachment; filename=analysis_results_" + jobId + ".csv")
                        .body(bytes);

            } catch (Exception e) {
                return ResponseEntity.internalServerError().<byte[]>build();
            }
        }).orElse(ResponseEntity.notFound().build());
    }
}
