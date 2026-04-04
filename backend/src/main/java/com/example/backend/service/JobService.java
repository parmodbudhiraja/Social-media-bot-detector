package com.example.backend.service;

import com.example.backend.model.AnalysisJob;
import com.example.backend.model.JobStatus;
import com.example.backend.repository.AnalysisJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class JobService {

    private static final Logger logger = LoggerFactory.getLogger(JobService.class);
    
    private final AnalysisJobRepository repository;
    private final SseService sseService;

    @Autowired
    public JobService(AnalysisJobRepository repository, SseService sseService) {
        this.repository = repository;
        this.sseService = sseService;
    }

    public AnalysisJob createJob(String url) {
        logger.info("Creating new AnalysisJob for URL: {}", url);
        AnalysisJob job = new AnalysisJob();
        job.setUrl(url);
        job.setStatus(JobStatus.PENDING);
        AnalysisJob saved = repository.save(job);
        logger.info("Job created with ID: {}", saved.getJobId());
        return saved;
    }

    public Optional<AnalysisJob> getJob(String jobId) {
        return repository.findById(jobId);
    }

    public AnalysisJob updateJobStatus(String jobId, JobStatus newStatus) {
        logger.info("Updating Job {} status to: {}", jobId, newStatus);
        AnalysisJob job = repository.findById(jobId)
            .orElseThrow(() -> {
                logger.error("Attempted to update non-existent Job: {}", jobId);
                return new IllegalArgumentException("Job not found: " + jobId);
            });
        
        job.setStatus(newStatus);
        AnalysisJob saved = repository.save(job);
        logger.debug("Job {} status persisted. Broadcasting via SSE.", jobId);
        sseService.sendUpdate(jobId, newStatus.name());
        return saved;
    }

    public void saveJobResult(String jobId, String resultJson) {
        logger.info("Saving ML inference result for Job {}", jobId);
        repository.findById(jobId).ifPresent(job -> {
            job.setResult(resultJson);
            repository.save(job);
        });
    }
}
