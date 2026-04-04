package com.example.backend.service;

import com.example.backend.model.AnalysisJob;
import com.example.backend.model.JobStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ApifyServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private WebClient webClient;

    @Mock
    private JobService jobService;

    @Mock
    private RabbitMQProducer rabbitMQProducer;

    @InjectMocks
    private ApifyService apifyService;

    private AnalysisJob job;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(apifyService, "apifyToken", "dummy-token");
        job = new AnalysisJob();
        job.setJobId(UUID.randomUUID().toString());
        job.setUrl("https://www.instagram.com/p/12345/");
        job.setStatus(JobStatus.PENDING);
        job.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void testTriggerScrapingSuccess() {
        Map<String, Object> mockData = Map.of("data", Map.of("id", "run-123"));
        
        when(webClient.post().uri(anyString()).bodyValue(any()).retrieve().bodyToMono(Map.class))
                .thenReturn(Mono.just(mockData));

        apifyService.triggerScraping(job);

        verify(jobService, times(1)).updateJobStatus(job.getJobId(), JobStatus.SCRAPING);
        // It triggers polling as well but we rely on simple mono chaining
    }

    @Test
    void testTriggerScrapingFailureMissingId() {
        Map<String, Object> mockData = Map.of("data", Map.of("wrong_key", "run-123"));
        
        when(webClient.post().uri(anyString()).bodyValue(any()).retrieve().bodyToMono(Map.class))
                .thenReturn(Mono.just(mockData));

        apifyService.triggerScraping(job);

        verify(jobService, times(1)).updateJobStatus(job.getJobId(), JobStatus.SCRAPING);
        verify(jobService, times(1)).updateJobStatus(job.getJobId(), JobStatus.FAILED);
    }

    @Test
    void testTriggerScrapingError() {
        when(webClient.post().uri(anyString()).bodyValue(any()).retrieve().bodyToMono(Map.class))
                .thenReturn(Mono.error(new RuntimeException("API Error")));

        apifyService.triggerScraping(job);

        verify(jobService, times(1)).updateJobStatus(job.getJobId(), JobStatus.SCRAPING);
        verify(jobService, times(1)).updateJobStatus(job.getJobId(), JobStatus.FAILED);
    }
}
