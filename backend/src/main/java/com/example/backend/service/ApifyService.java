package com.example.backend.service;

import com.example.backend.model.AnalysisJob;
import com.example.backend.model.JobStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ApifyService {

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
        jobService.updateJobStatus(job.getJobId(), JobStatus.SCRAPING);
        
        Map<String, Object> payload = Map.of(
            "directUrls", new String[]{job.getUrl()},
            "resultsLimit", 100
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
                    pollCommentRunStatus(runId, job.getJobId());
                } else {
                    failJob(job.getJobId(), "Failed to trigger comment scraper");
                }
            }, error -> failJob(job.getJobId(), error.getMessage()));
    }

    private void pollCommentRunStatus(String runId, String jobId) {
        Mono.delay(Duration.ofSeconds(10))
            .flatMap(l -> webClient.get()
                .uri("https://api.apify.com/v2/actor-runs/" + runId + "?token=" + apifyToken)
                .retrieve()
                .bodyToMono(Map.class))
            .subscribe(response -> {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                String status = (String) data.get("status");
                
                if ("SUCCEEDED".equals(status)) {
                    String defaultDatasetId = (String) data.get("defaultDatasetId");
                    fetchCommentsAndTriggerProfiles(defaultDatasetId, jobId);
                } else if ("FAILED".equals(status) || "ABORTED".equals(status)) {
                    failJob(jobId, "Comment scraper run failed");
                } else {
                    pollCommentRunStatus(runId, jobId);
                }
            }, error -> failJob(jobId, error.getMessage()));
    }

    /**
     * STAGE 1 Finish: Get comments, group by username
     */
    private void fetchCommentsAndTriggerProfiles(String datasetId, String jobId) {
        webClient.get()
            .uri("https://api.apify.com/v2/datasets/" + datasetId + "/items?token=" + apifyToken)
            .retrieve()
            .bodyToMono(Map[].class)
            .subscribe(items -> {
                if (items.length == 0) {
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

                triggerProfileScraping(userCommentsMap, jobId);
            }, error -> failJob(jobId, error.getMessage()));
    }

    /**
     * STAGE 2: Trigger Instagram Profile Scraper for gathered usernames
     */
    private void triggerProfileScraping(Map<String, List<Map<String, Object>>> userCommentsMap, String jobId) {
        List<String> usernames = new ArrayList<>(userCommentsMap.keySet());
        
        Map<String, Object> payload = Map.of(
            "usernames", usernames
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
                    failJob(jobId, "Failed to trigger profile scraper");
                }
            }, error -> failJob(jobId, error.getMessage()));
    }

    private void pollProfileRunStatus(String runId, Map<String, List<Map<String, Object>>> userCommentsMap, String jobId) {
        Mono.delay(Duration.ofSeconds(10))
            .flatMap(l -> webClient.get()
                .uri("https://api.apify.com/v2/actor-runs/" + runId + "?token=" + apifyToken)
                .retrieve()
                .bodyToMono(Map.class))
            .subscribe(response -> {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                String status = (String) data.get("status");
                
                if ("SUCCEEDED".equals(status)) {
                    String defaultDatasetId = (String) data.get("defaultDatasetId");
                    fetchProfilesAndSendToInference(defaultDatasetId, userCommentsMap, jobId);
                } else if ("FAILED".equals(status) || "ABORTED".equals(status)) {
                    failJob(jobId, "Profile scraper run failed");
                } else {
                    pollProfileRunStatus(runId, userCommentsMap, jobId);
                }
            }, error -> failJob(jobId, error.getMessage()));
    }

    /**
     * STAGE 2 Finish: Join everything and send to RabbitMQ
     */
    private void fetchProfilesAndSendToInference(String datasetId, Map<String, List<Map<String, Object>>> userCommentsMap, String jobId) {
        webClient.get()
            .uri("https://api.apify.com/v2/datasets/" + datasetId + "/items?token=" + apifyToken)
            .retrieve()
            .bodyToMono(Map[].class)
            .subscribe(profiles -> {
                jobService.updateJobStatus(jobId, JobStatus.INFERENCE);
                
                List<Map<String, Object>> finalItems = new ArrayList<>();

                for (Map<String, Object> profile : profiles) {
                    String username = (String) profile.get("username");
                    if (username != null && userCommentsMap.containsKey(username)) {
                        Map<String, Object> consolidatedItem = new HashMap<>();
                        consolidatedItem.put("profile", profile);
                        consolidatedItem.put("comments", userCommentsMap.get(username));
                        // Behavioral Model also likes likes (if available, otherwise empty)
                        consolidatedItem.put("likes", new ArrayList<>()); 
                        
                        finalItems.add(consolidatedItem);
                    }
                }

                Map<String, Object> rabbitPayload = Map.of(
                    "job_id", jobId,
                    "items", finalItems
                );
                
                rabbitMQProducer.sendToInferenceQueue(rabbitPayload);
            }, error -> failJob(jobId, error.getMessage()));
    }

    private void failJob(String jobId, String reason) {
        System.err.println("Job " + jobId + " failed: " + reason);
        jobService.updateJobStatus(jobId, JobStatus.FAILED);
    }
}
