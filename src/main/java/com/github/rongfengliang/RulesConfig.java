package com.github.rongfengliang;


import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

/**
 * @author dalong
 * rules文件配置信息
 */

@Data
public class RulesConfig {
    private String rulesId;
    private JsonNode rulesContent;
}
