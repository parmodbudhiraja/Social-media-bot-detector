package com.example.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SseService {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String jobId) {
        // Timeout set to 30 mins for long scraping tasks
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L); 
        
        emitter.onCompletion(() -> emitters.remove(jobId));
        emitter.onTimeout(() -> emitters.remove(jobId));
        emitter.onError(e -> emitters.remove(jobId));
        
        emitters.put(jobId, emitter);
        
        try {
            // Send initial connection event
            emitter.send(SseEmitter.event().name("CONNECT").data("Connected for job " + jobId));
        } catch (IOException e) {
            emitters.remove(jobId);
        }
        
        return emitter;
    }

    public void sendUpdate(String jobId, String status) {
        SseEmitter emitter = emitters.get(jobId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name("STATUS_UPDATE").data(status));
                if ("COMPLETED".equals(status) || "FAILED".equals(status)) {
                    emitter.complete();
                }
            } catch (IOException e) {
                emitters.remove(jobId);
            }
        }
    }
}
