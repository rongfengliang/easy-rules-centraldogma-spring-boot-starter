package com.github.rongfengliang;

import lombok.Data;

import java.util.List;

@Data
public class EasyRulesEntities {
    /**
     * rulesconfig can fetch from external storage
     */
    private  List<RulesConfig> rulesConfigs;
}
