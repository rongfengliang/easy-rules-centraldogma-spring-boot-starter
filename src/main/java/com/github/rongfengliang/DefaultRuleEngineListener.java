package com.github.rongfengliang;

import org.jeasy.rules.api.Facts;
import org.jeasy.rules.api.Rules;
import org.jeasy.rules.api.RulesEngineListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author dalong
 * 默认EngineListener
 */
public class DefaultRuleEngineListener implements RulesEngineListener {
    private Logger log = LoggerFactory.getLogger(DefaultRulesListener.class);

    @Override
    public void beforeEvaluate(Rules rules, Facts facts) {
        log.info("-----------------beforeEvaluate-----------------");
        log.info(" DefaultRuleEngineListener " + rules.toString() + " " + facts.toString());
    }

    @Override
    public void afterExecute(Rules rules, Facts facts) {
        log.info("-----------------afterExecute-----------------");
        log.info(" DefaultRuleEngineListener " + rules.toString() + "   " + facts.toString());
    }
}
