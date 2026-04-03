package com.example.backend.service;

import com.example.backend.config.RabbitMQConfig;
import com.example.backend.model.AnalysisJob;
import com.example.backend.model.JobStatus;
import com.example.backend.repository.AnalysisJobRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class RabbitMQConsumer {

    private final AnalysisJobRepository repository;
    private final ObjectMapper objectMapper;

    @Autowired
    public RabbitMQConsumer(AnalysisJobRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = RabbitMQConfig.RESULTS_QUEUE)
    public void receiveResults(Map<String, Object> results) {
        String jobId = (String) results.get("job_id");
        System.out.println("Received results for job: " + jobId);
        
        repository.findById(jobId).ifPresent(job -> {
            try {
                // Convert predictions to JSON string to store in result field
                String resultJson = objectMapper.writeValueAsString(results);
                job.setResult(resultJson);
                job.setStatus(JobStatus.COMPLETED);
                repository.save(job);
                System.out.println("Job " + jobId + " marked as COMPLETED.");
            } catch (Exception e) {
                System.err.println("Error saving results for job " + jobId + ": " + e.getMessage());
                job.setStatus(JobStatus.FAILED);
                repository.save(job);
            }
        });
    }
}
