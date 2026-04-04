package com.example.backend.service;

import com.example.backend.config.RabbitMQConfig;
import com.example.backend.model.AnalysisJob;
import com.example.backend.model.JobStatus;
import com.example.backend.repository.AnalysisJobRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class RabbitMQConsumer {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMQConsumer.class);
    
    private final AnalysisJobRepository repository;
    private final ObjectMapper objectMapper;
    private final SseService sseService;

    @Autowired
    public RabbitMQConsumer(AnalysisJobRepository repository, ObjectMapper objectMapper, SseService sseService) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.sseService = sseService;
    }

    @RabbitListener(queues = RabbitMQConfig.RESULTS_QUEUE)
    public void receiveResults(Map<String, Object> results) {
        String jobId = (String) results.get("job_id");
        logger.info("Job {}: Received inference results from ML service", jobId);
        
        repository.findById(jobId).ifPresentOrElse(job -> {
            try {
                String resultJson = objectMapper.writeValueAsString(results);
                logger.debug("Job {}: Persisting results ({} bytes)", jobId, resultJson.length());
                
                job.setResult(resultJson);
                job.setStatus(JobStatus.COMPLETED);
                repository.save(job);
                
                logger.info("Job {}: Status updated to COMPLETED. Broadcasting SSE.", jobId);
                sseService.sendUpdate(jobId, "COMPLETED");
            } catch (Exception e) {
                logger.error("Job {}: Failed to save results: {}", jobId, e.getMessage());
                job.setStatus(JobStatus.FAILED);
                repository.save(job);
                sseService.sendUpdate(jobId, "FAILED");
            }
        }, () -> logger.warn("Job {}: Received results for non-existent job ID", jobId));
    }
}
