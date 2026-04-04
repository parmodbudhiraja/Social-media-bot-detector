package com.example.backend.controller;

import com.example.backend.model.AnalysisJob;
import com.example.backend.model.JobStatus;
import com.example.backend.service.ApifyService;
import com.example.backend.service.FileGeneratorService;
import com.example.backend.service.JobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(JobController.class)
public class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JobService jobService;

    @MockBean
    private ApifyService apifyService;

    @MockBean
    private FileGeneratorService fileGeneratorService;

    private AnalysisJob mockJob;

    @BeforeEach
    void setUp() {
        mockJob = new AnalysisJob();
        mockJob.setJobId(UUID.randomUUID().toString());
        mockJob.setUrl("https://www.instagram.com/p/validid/");
        mockJob.setStatus(JobStatus.PENDING);
    }

    @Test
    void testStartJobSuccess() throws Exception {
        when(jobService.createJob(anyString())).thenReturn(mockJob);

        mockMvc.perform(post("/api/jobs/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"https://www.instagram.com/p/validid/\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").value(mockJob.getJobId()));

        verify(apifyService, times(1)).triggerScraping(mockJob);
    }

    @Test
    void testStartJobMalformedUrl() throws Exception {
        mockMvc.perform(post("/api/jobs/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"not-a-url\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid URL format"));

        verify(jobService, never()).createJob(anyString());
    }

    @Test
    void testGetJobStatusNotFound() throws Exception {
        when(jobService.getJob(anyString())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/jobs/nonexistent-id/status"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetJobStatusFound() throws Exception {
        when(jobService.getJob(anyString())).thenReturn(Optional.of(mockJob));

        mockMvc.perform(get("/api/jobs/" + mockJob.getJobId() + "/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void testGetJobResults() throws Exception {
        mockJob.setStatus(JobStatus.COMPLETED);
        mockJob.setResult("{\"data\":\"mock\"}");
        when(jobService.getJob(anyString())).thenReturn(Optional.of(mockJob));

        mockMvc.perform(get("/api/jobs/" + mockJob.getJobId() + "/results"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"data\":\"mock\"}"));
    }

    @Test
    void testGetJobResultsNotCompleted() throws Exception {
        when(jobService.getJob(anyString())).thenReturn(Optional.of(mockJob));

        mockMvc.perform(get("/api/jobs/" + mockJob.getJobId() + "/results"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Results not available or job failed"));
    }
}
