package com.example.backend.service;

import com.example.backend.model.AnalysisJob;
import com.example.backend.model.JobStatus;
import com.example.backend.repository.AnalysisJobRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RabbitMQConsumerTest {

    @Mock
    private AnalysisJobRepository repository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private SseService sseService;

    @InjectMocks
    private RabbitMQConsumer rabbitMQConsumer;

    private Map<String, Object> mockResults;
    private AnalysisJob job;

    @BeforeEach
    void setUp() {
        mockResults = new HashMap<>();
        mockResults.put("job_id", "job-123");
        mockResults.put("predictions", "[]");
        
        job = new AnalysisJob();
        job.setJobId("job-123");
        job.setStatus(JobStatus.INFERENCE);
    }

    @Test
    void testReceiveResultsSuccess() throws JsonProcessingException {
        when(repository.findById("job-123")).thenReturn(Optional.of(job));
        when(objectMapper.writeValueAsString(mockResults)).thenReturn("{\"json\":\"mock\"}");

        rabbitMQConsumer.receiveResults(mockResults);

        verify(repository, times(1)).save(job);
        verify(sseService, times(1)).sendUpdate("job-123", "COMPLETED");
        assert job.getStatus() == JobStatus.COMPLETED;
        assert job.getResult().equals("{\"json\":\"mock\"}");
    }

    @Test
    void testReceiveResultsNonExistentJob() {
        when(repository.findById("job-123")).thenReturn(Optional.empty());

        rabbitMQConsumer.receiveResults(mockResults);

        verify(repository, never()).save(any());
        verify(sseService, never()).sendUpdate(anyString(), anyString());
    }

    @Test
    void testReceiveResultsJsonProcessingException() throws JsonProcessingException {
        when(repository.findById("job-123")).thenReturn(Optional.of(job));
        when(objectMapper.writeValueAsString(mockResults)).thenThrow(new JsonProcessingException("Error") {});

        rabbitMQConsumer.receiveResults(mockResults);

        verify(repository, times(1)).save(job);
        verify(sseService, times(1)).sendUpdate("job-123", "FAILED");
        assert job.getStatus() == JobStatus.FAILED;
    }
}
