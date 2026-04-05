package com.example.backend.service;

import com.example.backend.model.AnalysisJob;
import com.example.backend.model.JobStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ApifyService {

    private static final Logger logger = LoggerFactory.getLogger(ApifyService.class);
    
    private final WebClient webClient;
    private final JobService jobService;
    private final RabbitMQProducer rabbitMQProducer;

    @Value("${apify.token}")
    private String apifyToken;

    @Autowired
    public ApifyService(WebClient webClient, JobService jobService, RabbitMQProducer rabbitMQProducer) {
        this.webClient = webClient;
        this.jobService = jobService;
        this.rabbitMQProducer = rabbitMQProducer;
    }

    /**
     * STAGE 1: Trigger Instagram Comment Scraper
     */
    public void triggerScraping(AnalysisJob job) {
        logger.info("Job {}: Initiating Phase 5 Stage 1 - Comment Scraping for URL: {}", job.getJobId(), job.getUrl());
        jobService.updateJobStatus(job.getJobId(), JobStatus.SCRAPING);
        
        Map<String, Object> payload = Map.of(
            "directUrls", List.of(job.getUrl()),
            "resultsLimit", 50,
            "proxyConfiguration", Map.of("useApifyProxy", true)
        );

        webClient.post()
            .uri("https://api.apify.com/v2/acts/apify~instagram-comment-scraper/runs?token=" + apifyToken)
            .bodyValue(payload)
            .retrieve()
            .bodyToMono(Map.class)
            .subscribe(response -> {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                if (data != null && data.containsKey("id")) {
                    String runId = (String) data.get("id");
                    logger.info("Job {}: Comment scraper triggered successfully. Run ID: {}", job.getJobId(), runId);
                    pollCommentRunStatus(runId, job.getJobId());
                } else {
                    logger.error("Job {}: Failed to trigger comment scraper, response missing run ID", job.getJobId());
                    failJob(job.getJobId(), "Failed to trigger comment scraper");
                }
            }, error -> {
                logger.error("Job {}: Error triggering comment scraper: {}", job.getJobId(), error.getMessage());
                failJob(job.getJobId(), error.getMessage());
            });
    }

    private void pollCommentRunStatus(String runId, String jobId) {
        logger.debug("Job {}: Polling status for comment scraper run: {}", jobId, runId);
        Mono.delay(Duration.ofSeconds(10))
            .flatMap(l -> webClient.get()
                .uri("https://api.apify.com/v2/actor-runs/" + runId + "?token=" + apifyToken)
                .retrieve()
                .bodyToMono(Map.class))
            .subscribe(response -> {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                String status = (String) data.get("status");
                
                logger.debug("Job {}: Comment run {} status: {}", jobId, runId, status);
                
                if ("SUCCEEDED".equals(status)) {
                    String defaultDatasetId = (String) data.get("defaultDatasetId");
                    logger.info("Job {}: Comment run {} succeeded. Fetching dataset {}", jobId, runId, defaultDatasetId);
                    fetchCommentsAndTriggerProfiles(defaultDatasetId, jobId);
                } else if ("FAILED".equals(status) || "ABORTED".equals(status)) {
                    logger.error("Job {}: Comment run {} failed or aborted", jobId, runId);
                    failJob(jobId, "Comment scraper run failed");
                } else {
                    pollCommentRunStatus(runId, jobId);
                }
            }, error -> {
                logger.error("Job {}: Error polling comment run status: {}", jobId, error.getMessage());
                failJob(jobId, error.getMessage());
            });
    }

    /**
     * STAGE 1 Finish: Get comments, group by username
     */
    private void fetchCommentsAndTriggerProfiles(String datasetId, String jobId) {
        logger.info("Job {}: Fetching comment items from dataset {}", jobId, datasetId);
        webClient.get()
            .uri("https://api.apify.com/v2/datasets/" + datasetId + "/items?token=" + apifyToken)
            .retrieve()
            .bodyToMono(Map[].class)
            .subscribe(items -> {
                logger.info("Job {}: Received {} comments from stage 1", jobId, items.length);
                if (items.length == 0) {
                    logger.warn("Job {}: Stage 1 result is empty, aborting.", jobId);
                    failJob(jobId, "No comments found on this post");
                    return;
                }

                // Group comments by ownerUsername
                Map<String, List<Map<String, Object>>> userCommentsMap = new HashMap<>();
                for (Map<String, Object> item : items) {
                    String username = (String) item.get("ownerUsername");
                    if (username != null) {
                        userCommentsMap.computeIfAbsent(username, k -> new ArrayList<>()).add(item);
                    }
                }

                logger.info("Job {}: Grouped comments for {} unique users. Triggering Stage 2 (Profiles).", jobId, userCommentsMap.size());
                triggerProfileScraping(userCommentsMap, jobId);
            }, error -> {
                logger.error("Job {}: Error fetching comments dataset: {}", jobId, error.getMessage());
                failJob(jobId, error.getMessage());
            });
    }

    /**
     * STAGE 2: Trigger Instagram Profile Scraper for gathered usernames
     */
    private void triggerProfileScraping(Map<String, List<Map<String, Object>>> userCommentsMap, String jobId) {
        List<String> usernames = new ArrayList<>(userCommentsMap.keySet());
        
        Map<String, Object> payload = Map.of(
            "usernames", usernames,
            "proxyConfiguration", Map.of("useApifyProxy", true)
        );

        webClient.post()
            .uri("https://api.apify.com/v2/acts/apify~instagram-profile-scraper/runs?token=" + apifyToken)
            .bodyValue(payload)
            .retrieve()
            .bodyToMono(Map.class)
            .subscribe(response -> {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                if (data != null && data.containsKey("id")) {
                    String runId = (String) data.get("id");
                    pollProfileRunStatus(runId, userCommentsMap, jobId);
                } else {
                    logger.error("Job {}: Failed to trigger profile scraper, response data mission id", jobId);
                    failJob(jobId, "Failed to trigger profile scraper");
                }
            }, error -> {
                logger.error("Job {}: Error triggering profile scraper: {}", jobId, error.getMessage());
                failJob(jobId, error.getMessage());
            });
    }

    private void pollProfileRunStatus(String runId, Map<String, List<Map<String, Object>>> userCommentsMap, String jobId) {
        logger.debug("Job {}: Polling profile scraper run status for {}", jobId, runId);
        Mono.delay(Duration.ofSeconds(10))
            .flatMap(l -> webClient.get()
                .uri("https://api.apify.com/v2/actor-runs/" + runId + "?token=" + apifyToken)
                .retrieve()
                .bodyToMono(Map.class))
            .subscribe(response -> {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                String status = (String) data.get("status");
                
                logger.debug("Job {}: Profile scraper run {} status: {}", jobId, runId, status);
                
                if ("SUCCEEDED".equals(status)) {
                    String defaultDatasetId = (String) data.get("defaultDatasetId");
                    logger.info("Job {}: Profile scraper run {} succeeded, fetching dataset {}", jobId, runId, defaultDatasetId);
                    fetchProfilesAndSendToInference(defaultDatasetId, userCommentsMap, jobId);
                } else if ("FAILED".equals(status) || "ABORTED".equals(status)) {
                    logger.error("Job {}: Profile scraper run {} failed or aborted", jobId, runId);
                    failJob(jobId, "Profile scraper run failed");
                } else {
                    pollProfileRunStatus(runId, userCommentsMap, jobId);
                }
            }, error -> {
                logger.error("Job {}: Error polling profile scraper run status: {}", jobId, error.getMessage());
                failJob(jobId, error.getMessage());
            });
    }

    /**
     * STAGE 2 Finish: Join everything and send to RabbitMQ
     */
    private void fetchProfilesAndSendToInference(String datasetId, Map<String, List<Map<String, Object>>> userCommentsMap, String jobId) {
        logger.info("Job {}: Fetching final profile items from dataset {}", jobId, datasetId);
        webClient.get()
            .uri("https://api.apify.com/v2/datasets/" + datasetId + "/items?token=" + apifyToken)
            .retrieve()
            .bodyToMono(Map[].class)
            .subscribe(profiles -> {
                logger.info("Job {}: Received {} profiles to merge with {} user comment groups", jobId, profiles.length, userCommentsMap.size());
                jobService.updateJobStatus(jobId, JobStatus.INFERENCE);
                
                List<Map<String, Object>> finalItems = new ArrayList<>();

                for (Map<String, Object> profile : profiles) {
                    String username = (String) profile.get("username");
                    if (username != null && userCommentsMap.containsKey(username)) {
                        Map<String, Object> consolidatedItem = new HashMap<>();
                        consolidatedItem.put("profile", profile);
                        consolidatedItem.put("comments", userCommentsMap.get(username));
                        consolidatedItem.put("likes", new ArrayList<>()); 
                        finalItems.add(consolidatedItem);
                    }
                }

                logger.info("Job {}: Sending {} joined items to RabbitMQ inference queue", jobId, finalItems.size());
                Map<String, Object> rabbitPayload = Map.of(
                    "job_id", jobId,
                    "items", finalItems
                );
                
                rabbitMQProducer.sendToInferenceQueue(rabbitPayload);
            }, error -> {
                logger.error("Job {}: Error fetching final profile dataset: {}", jobId, error.getMessage());
                failJob(jobId, error.getMessage());
            });
    }

    private void failJob(String jobId, String reason) {
        logger.error("CRITICAL ERROR: Job {} failed. Reason: {}", jobId, reason);
        jobService.updateJobStatus(jobId, JobStatus.FAILED);
    }
}
