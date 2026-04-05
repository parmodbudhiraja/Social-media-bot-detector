package com.example.backend.service;

import com.example.backend.config.RabbitMQConfig;
import com.example.backend.model.JobStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RabbitMQListener {

    private final TruthTableService truthTableService;
    private final FileGeneratorService fileGeneratorService;
    private final JobService jobService;
    private final SseService sseService;
    private final ObjectMapper objectMapper;

    @Autowired
    public RabbitMQListener(TruthTableService truthTableService, 
                            FileGeneratorService fileGeneratorService, 
                            JobService jobService,
                            SseService sseService,
                            ObjectMapper objectMapper) {
        this.truthTableService = truthTableService;
        this.fileGeneratorService = fileGeneratorService;
        this.jobService = jobService;
        this.sseService = sseService;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = RabbitMQConfig.RESULTS_QUEUE)
    public void consumeResults(Map<String, Object> message) {
        String jobId = (String) message.get("job_id");
        System.out.println("Backend: Received results from RabbitMQ for Job " + jobId);
        List<Integer> xPreds = (List<Integer>) message.get("x_predictions");
        List<Integer> yPreds = (List<Integer>) message.get("y_predictions");
        
        List<String> usernames = (List<String>) message.get("usernames");
        if (usernames == null) {
            usernames = new ArrayList<>();
            for (int i = 0; i < xPreds.size(); i++) {
                usernames.add("user_" + i);
            }
        }

        Map<TruthTableService.CategorizationResult, List<String>> categorized = new HashMap<>();
        for (TruthTableService.CategorizationResult res : TruthTableService.CategorizationResult.values()) {
            categorized.put(res, new ArrayList<>());
        }

        for (int i = 0; i < xPreds.size(); i++) {
            int x = xPreds.get(i);
            int y = yPreds.get(i);
            
            TruthTableService.CategorizationResult result = truthTableService.intersect(x, y);
            
            String username = i < usernames.size() ? usernames.get(i) : "unknown_" + i;
            categorized.get(result).add(username);
        }

        try {
            String jsonResult = objectMapper.writeValueAsString(message);
            jobService.saveJobResult(jobId, jsonResult);
        } catch (Exception e) {
            e.printStackTrace();
        }

        fileGeneratorService.generateCategorizedFiles(jobId, categorized);
        jobService.updateJobStatus(jobId, JobStatus.COMPLETED);
        sseService.sendUpdate(jobId, JobStatus.COMPLETED.name());
    }
}
