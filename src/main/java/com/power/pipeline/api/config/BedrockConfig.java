package com.power.pipeline.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "aws.bedrock")
@Data
public class BedrockConfig {

    private String region;
    private String accessKeyId;
    private String secretAccessKey;
    private String modelId;
    private boolean enabled = false;  // Disabled by default

    public boolean isConfigured() {
        return enabled &&
               accessKeyId != null && !accessKeyId.isEmpty() &&
               secretAccessKey != null && !secretAccessKey.isEmpty();
    }
}
