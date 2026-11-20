package com.power.pipeline.api.controller;

import com.power.pipeline.common.service.PipelineOrchestrator;
import com.power.pipeline.common.service.PipelineOrchestrator.PipelineResult;
import com.power.pipeline.gold.entity.GoldPowerData;
import com.power.pipeline.gold.repository.GoldPowerDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pipeline")
@RequiredArgsConstructor
public class PipelineController {

    private final PipelineOrchestrator pipelineOrchestrator;
    private final GoldPowerDataRepository goldRepository;

    @PostMapping("/run")
    public ResponseEntity<PipelineResult> runPipeline() {
        PipelineResult result = pipelineOrchestrator.runFullPipeline();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/data")
    public ResponseEntity<List<GoldPowerData>> getData(
            @RequestParam(required = false) String state,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        List<GoldPowerData> data;

        if (state != null) {
            data = goldRepository.findByStateOrderByReportDateDesc(state);
        } else if (startDate != null && endDate != null) {
            data = goldRepository.findByReportDateBetween(startDate, endDate);
        } else {
            data = goldRepository.findAll();
        }

        return ResponseEntity.ok(data);
    }

    @GetMapping("/data/quality-issues")
    public ResponseEntity<List<GoldPowerData>> getQualityIssues(
            @RequestParam(defaultValue = "95.0") Double agreementThreshold
    ) {
        List<GoldPowerData> issues = goldRepository.findRecordsWithSourceDisagreement(agreementThreshold);
        return ResponseEntity.ok(issues);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        long totalRecords = goldRepository.count();
        List<GoldPowerData> allData = goldRepository.findAll();

        long highConfidence = allData.stream()
                .filter(d -> "HIGH".equals(d.getConfidenceLevel()))
                .count();

        long mediumConfidence = allData.stream()
                .filter(d -> "MEDIUM".equals(d.getConfidenceLevel()))
                .count();

        long lowConfidence = allData.stream()
                .filter(d -> "LOW".equals(d.getConfidenceLevel()))
                .count();

        double avgQualityScore = allData.stream()
                .mapToDouble(d -> d.getDataQualityScore() != null ? d.getDataQualityScore() : 0.0)
                .average()
                .orElse(0.0);

        return ResponseEntity.ok(Map.of(
                "totalRecords", totalRecords,
                "highConfidence", highConfidence,
                "mediumConfidence", mediumConfidence,
                "lowConfidence", lowConfidence,
                "averageQualityScore", avgQualityScore
        ));
    }
}
