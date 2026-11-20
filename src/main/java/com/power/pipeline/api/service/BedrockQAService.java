package com.power.pipeline.api.service;

import com.power.pipeline.api.config.BedrockConfig;
import com.power.pipeline.gold.entity.GoldPowerData;
import com.power.pipeline.gold.repository.GoldPowerDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;
import software.amazon.awssdk.core.SdkBytes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class BedrockQAService {

    private final GoldPowerDataRepository goldRepository;
    private final BedrockConfig bedrockConfig;
    private final ObjectMapper objectMapper;

    public String answerQuestion(String question) {
        if (!bedrockConfig.isConfigured()) {
            return "AWS Bedrock is not configured. Please set AWS credentials in environment variables.";
        }

        try {
            // 1. Get data from gold layer
            List<GoldPowerData> allData = goldRepository.findAll();

            if (allData.isEmpty()) {
                return "No data available to answer questions.";
            }

            // 2. Build context
            String dataContext = buildDataContext(allData);

            // 3. Build prompt
            String systemPrompt = """
                You are a data analyst assistant for an electricity data pipeline.

                Your role:
                - Answer questions about state-level electricity data
                - Always cite data quality metrics (confidence levels, quality scores)
                - Communicate data limitations clearly
                - Use markdown formatting for better readability

                Data Limitations to ALWAYS mention:
                - 7-day sample period (Nov 13-19, 2026)
                - State-level daily aggregates only (no intraday data)
                - 1-2 day publication delay
                - State-level only (no city/district breakdown)

                Format responses with:
                - Clear answer first
                - Supporting data/statistics
                - **Data Quality Note:** section
                - **Limitations:** section (when relevant)
                """;

            String userPrompt = String.format("""
                Data Context:
                %s

                Question: %s

                Provide a clear, concise answer using only the data above.
                MUST include data quality notes and limitations.
                """, dataContext, question);

            // 4. Call AWS Bedrock
            String response = invokeClaude(systemPrompt, userPrompt);

            return response;

        } catch (Exception e) {
            log.error("Error calling AWS Bedrock", e);
            return "Error: " + e.getMessage() + "\n\nFalling back to rule-based mode. Try without ?mode=llm";
        }
    }

    private String invokeClaude(String systemPrompt, String userPrompt) throws Exception {
        // Create Bedrock client
        BedrockRuntimeClient client = BedrockRuntimeClient.builder()
                .region(Region.of(bedrockConfig.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(
                        bedrockConfig.getAccessKeyId(),
                        bedrockConfig.getSecretAccessKey()
                    )
                ))
                .build();

        // Build request body for Claude
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("anthropic_version", "bedrock-2023-05-31");
        requestBody.put("max_tokens", 2000);

        // Add messages
        var messages = requestBody.putArray("messages");
        var message = messages.addObject();
        message.put("role", "user");
        var content = message.putArray("content");
        content.addObject().put("type", "text").put("text", systemPrompt + "\n\n" + userPrompt);

        // Invoke model
        InvokeModelRequest request = InvokeModelRequest.builder()
                .modelId(bedrockConfig.getModelId())
                .body(SdkBytes.fromUtf8String(requestBody.toString()))
                .build();

        InvokeModelResponse response = client.invokeModel(request);

        // Parse response
        String responseBody = response.body().asUtf8String();
        var jsonResponse = objectMapper.readTree(responseBody);

        return jsonResponse.get("content").get(0).get("text").asText();
    }

    private String buildDataContext(List<GoldPowerData> data) {
        StringBuilder context = new StringBuilder();

        context.append("Dataset Summary:\n");
        context.append(String.format("- Total Records: %d\n", data.size()));
        context.append(String.format("- States: %s\n",
            data.stream().map(GoldPowerData::getState).distinct()
                .collect(Collectors.joining(", "))));
        context.append(String.format("- Date Range: %s to %s\n\n",
            data.stream().map(GoldPowerData::getReportDate).min((a,b) -> a.compareTo(b)).orElse(null),
            data.stream().map(GoldPowerData::getReportDate).max((a,b) -> a.compareTo(b)).orElse(null)));

        context.append("Detailed Records:\n");
        for (GoldPowerData d : data) {
            context.append(String.format(
                "- %s, %s: Required=%.2f MU, Supplied=%.2f MU, Shortage=%.2f MU (%.2f%%), " +
                "Source=%s, Confidence=%s, Quality=%.2f\n",
                d.getState(), d.getReportDate(),
                d.getEnergyRequiredMu(), d.getEnergySuppliedMu(),
                d.getEnergyShortfallMu(), d.getShortfallPercentage(),
                d.getPrimarySource(), d.getConfidenceLevel(), d.getDataQualityScore()
            ));
        }

        return context.toString();
    }
}
