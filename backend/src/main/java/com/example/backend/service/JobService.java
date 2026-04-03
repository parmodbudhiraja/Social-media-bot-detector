package com.example.backend.service;

import com.example.backend.model.AnalysisJob;
import com.example.backend.model.JobStatus;
import com.example.backend.repository.AnalysisJobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class JobService {
    
    private final AnalysisJobRepository repository;

    @Autowired
    public JobService(AnalysisJobRepository repository) {
        this.repository = repository;
    }

    public AnalysisJob createJob(String url) {
        AnalysisJob job = new AnalysisJob();
        job.setUrl(url);
        job.setStatus(JobStatus.PENDING);
        return repository.save(job);
    }

    public Optional<AnalysisJob> getJob(String jobId) {
        return repository.findById(jobId);
    }

    public AnalysisJob updateJobStatus(String jobId, JobStatus newStatus) {
        AnalysisJob job = repository.findById(jobId)
            .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
        
        job.setStatus(newStatus);
        return repository.save(job);
    }
}
