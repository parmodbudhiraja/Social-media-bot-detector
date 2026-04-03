package com.example.backend.service;

import org.springframework.stereotype.Service;

@Service
public class TruthTableService {

    public enum CategorizationResult {
        CERTAINLY_FAKE,
        MAY_BE_FAKE,
        MAY_BE_REAL,
        CERTAINLY_REAL
    }

    /**
     * Categorizes a user based on Profile Metric ML model (x) and Behavioral Sequence ML model (y).
     * Assuming convention: 1 = fake, 0 = real.
     * x: Profile metrics show bot traits?
     * y: Sequence metrics show bot layout?
     */
    public CategorizationResult intersect(int xPrediction, int yPrediction) {
        if (xPrediction == 1 && yPrediction == 1) {
            return CategorizationResult.CERTAINLY_FAKE;
        } else if (xPrediction == 1 && yPrediction == 0) {
            return CategorizationResult.MAY_BE_FAKE;
        } else if (xPrediction == 0 && yPrediction == 1) {
            return CategorizationResult.MAY_BE_REAL; // Behavioral anomaly but healthy profile
        } else {
            return CategorizationResult.CERTAINLY_REAL;
        }
    }
}
