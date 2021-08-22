package com.github.rongfengliang;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;


@Data
@ConfigurationProperties(prefix = "easyrules")
public class EasyRulesEngineConfiguration {
    private boolean skipOnFirstAppliedRule;
    private boolean skipOnFirstNonTriggeredRule;
    private boolean skipOnFirstFailedRule;
    private int priorityThreshold;
    private String project;
    private boolean template; // spel default template_expression
    private String repo;
    private String contentType;
    private String confName;
}
