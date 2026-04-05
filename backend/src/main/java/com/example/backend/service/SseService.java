package com.example.backend.service;

import com.example.backend.model.AnalysisJob;
import com.example.backend.repository.AnalysisJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SseService {

    private static final Logger logger = LoggerFactory.getLogger(SseService.class);
    
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final AnalysisJobRepository repository;

    @Autowired
    public SseService(AnalysisJobRepository repository) {
        this.repository = repository;
    }

    public SseEmitter subscribe(String jobId) {
        logger.info("Job {}: New SSE subscription request", jobId);
        // Timeout set to 30 mins for long scraping tasks
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L); 
        
        emitter.onCompletion(() -> {
            logger.info("Job {}: SSE connection completed", jobId);
            emitters.remove(jobId);
        });
        emitter.onTimeout(() -> {
            logger.warn("Job {}: SSE connection timed out", jobId);
            emitters.remove(jobId);
        });
        emitter.onError(e -> {
            logger.error("Job {}: SSE connection error: {}", jobId, e.getMessage());
            emitters.remove(jobId);
        });
        
        emitters.put(jobId, emitter);
        
        try {
            emitter.send(SseEmitter.event().name("CONNECT").data("Connected for job " + jobId));
            
            // Send a ping every 25 seconds to keep the Railway connection alive
            new Thread(() -> {
                while (emitters.containsKey(jobId)) {
                    try {
                        Thread.sleep(25000);
                        SseEmitter e = emitters.get(jobId);
                        if (e != null) {
                            e.send(SseEmitter.event().name("PING").data("keep-alive"));
                        }
                    } catch (Exception ex) {
                        break;
                    }
                }
            }).start();

            repository.findById(jobId).ifPresent(job -> {
                try {
                    logger.debug("Job {}: Sending initial status sync: {}", jobId, job.getStatus());
                    emitter.send(SseEmitter.event().name("STATUS_UPDATE").data(job.getStatus().name()));
                } catch (IOException ignored) {}
            });
            
        } catch (IOException e) {
            logger.error("Job {}: Failed to send initial SSE CONNECT event", jobId);
            emitters.remove(jobId);
        }
        
        return emitter;
    }

    public void sendUpdate(String jobId, String status) {
        SseEmitter emitter = emitters.get(jobId);
        if (emitter != null) {
            try {
                logger.debug("Job {}: Broadcasting status update: {}", jobId, status);
                emitter.send(SseEmitter.event().name("STATUS_UPDATE").data(status));
                if ("COMPLETED".equals(status) || "FAILED".equals(status)) {
                    logger.info("Job {}: Final status reached ({}), closing SSE.", jobId, status);
                    emitter.complete();
                }
            } catch (IOException e) {
                logger.warn("Job {}: Failed to broadcast update, removing stale emitter.", jobId);
                emitters.remove(jobId);
            }
        } else {
            logger.debug("Job {}: No active SSE emitter to receive update: {}", jobId, status);
        }
    }
}
