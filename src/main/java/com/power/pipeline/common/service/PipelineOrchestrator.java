package com.power.pipeline.common.service;

import com.power.pipeline.bronze.service.BronzeIngestionService;
import com.power.pipeline.gold.service.GoldAggregationService;
import com.power.pipeline.silver.service.SilverProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PipelineOrchestrator {

    private final BronzeIngestionService bronzeService;
    private final SilverProcessingService silverService;
    private final GoldAggregationService goldService;

    public PipelineResult runFullPipeline() {
        log.info("========================================");
        log.info("Starting Full Pipeline Execution");
        log.info("========================================");

        long startTime = System.currentTimeMillis();
        PipelineResult result = new PipelineResult();

        try {
            // Stage 1: Bronze ingestion
            log.info("Stage 1: Bronze Ingestion");
            int bronzeCount = bronzeService.ingestRawFiles();
            result.setBronzeRecords(bronzeCount);
            log.info("Bronze ingestion completed: {} records", bronzeCount);

            // Stage 2: Silver processing
            log.info("Stage 2: Silver Processing");
            int silverCount = silverService.processBronzeToSilver();
            result.setSilverRecords(silverCount);
            log.info("Silver processing completed: {} records", silverCount);

            // Stage 3: Gold aggregation
            log.info("Stage 3: Gold Aggregation");
            int goldCount = goldService.aggregateSilverToGold();
            result.setGoldRecords(goldCount);
            log.info("Gold aggregation completed: {} records", goldCount);

            long duration = System.currentTimeMillis() - startTime;
            result.setDurationMs(duration);
            result.setSuccess(true);

            log.info("========================================");
            log.info("Pipeline Execution Completed Successfully");
            log.info("Duration: {} ms", duration);
            log.info("Bronze: {}, Silver: {}, Gold: {}", bronzeCount, silverCount, goldCount);
            log.info("========================================");

        } catch (Exception e) {
            log.error("Pipeline execution failed", e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }

    @lombok.Data
    public static class PipelineResult {
        private boolean success;
        private int bronzeRecords;
        private int silverRecords;
        private int goldRecords;
        private long durationMs;
        private String errorMessage;
    }
}
