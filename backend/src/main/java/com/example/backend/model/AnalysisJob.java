package com.example.backend.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Enumerated;
import javax.persistence.EnumType;
import org.hibernate.annotations.GenericGenerator;
import java.time.LocalDateTime;

@Entity
public class AnalysisJob {
    
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    private String jobId;
    
    private String url;
    
    @Enumerated(EnumType.STRING)
    private JobStatus status;
    
    private LocalDateTime createdAt;
    
    @javax.persistence.Column(columnDefinition = "TEXT")
    private String result;
    
    public AnalysisJob() {
        this.createdAt = LocalDateTime.now();
        this.status = JobStatus.PENDING;
    }
    
    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
}
