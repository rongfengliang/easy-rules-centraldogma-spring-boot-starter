package com.github.rongfengliang;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linecorp.centraldogma.client.CentralDogma;
import com.linecorp.centraldogma.client.Watcher;
import com.linecorp.centraldogma.common.Entry;
import com.linecorp.centraldogma.common.Query;
import com.linecorp.centraldogma.common.Revision;
import org.jeasy.rules.api.*;
import org.jeasy.rules.core.DefaultRulesEngine;
import org.jeasy.rules.spel.SpELRuleFactory;
import org.jeasy.rules.support.reader.JsonRuleDefinitionReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.expression.BeanResolver;
import org.springframework.expression.ParserContext;

import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Configuration
@EnableConfigurationProperties(EasyRulesEngineConfiguration.class)
public class EasyRulesAutoConfiguration {
    private Logger log = LoggerFactory.getLogger(DefaultRulesListener.class);
    private final EasyRulesEngineConfiguration properties;
    private Map<String, Rules> easyRules;
    private EasyRulesEntities easyRulesEntities;
    EasyRulesAutoConfiguration(EasyRulesEngineConfiguration properties) {
        this.properties = properties;
        this.easyRules = null;
        this.easyRulesEntities =new EasyRulesEntities();
    }

    @Bean
    @ConditionalOnMissingBean
    public RuleListener defaultRulesListener() {
        return new DefaultRulesListener();
    }

    @Bean
    @ConditionalOnMissingBean
    public RulesEngineListener defaultRuleEngineListener() {
        return new DefaultRuleEngineListener();
    }

    @Bean
    @ConditionalOnMissingBean
    public BeanResolver defaultedResolver(SpringBeanUtil springBeanUtil) {
        return new SimpleBeanResovler(SpringBeanUtil.getApplicationContext());
    }

    @Bean
    @ConditionalOnMissingBean
    public SpringBeanUtil springBeanUtil() {
        return new SpringBeanUtil();
    }

    @Bean
    @ConditionalOnClass(value = {CentralDogma.class, ObjectMapper.class})
    public CommandLineRunner commandLineRunner(CentralDogma dogma, ObjectMapper objectMapper, BeanResolver beanResolver) {
        log.info("project:{}, repo:{},filename:{}", properties.getProject(), properties.getRepo(), properties.getConfName());
        Watcher watcher = dogma.fileWatcher(properties.getProject(), properties.getRepo(), Query.ofText(properties.getConfName()));
        watcher.watch((revision, value) -> {
            log.info("Updated to {} at {}", value, revision);
            try {
                List<RulesConfig> rulesConfigs = objectMapper.readValue(value.toString(), new TypeReference<List<RulesConfig>>() {
                });
                easyRulesEntities.setRulesConfigs(rulesConfigs);
                this.easyRules = rulesContent2EasyRules(beanResolver);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                log.error("json convert  error", e);
            }
        });
        return args -> {
            log.info("init CentralDogma load easy rules conf");
        };
    }

    /**
     * centralDogmaRules with prototype for fetch new conf rules
     * bean name is <b>centralDogmaRules</b>
     *
     * @param beanResolver
     * @param centralDogma
     * @return
     * @throws Exception
     */
    @Bean(name = "centralDogmaRules")
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE,proxyMode = ScopedProxyMode.TARGET_CLASS)
    @ConditionalOnClass(value = {CentralDogma.class, ObjectMapper.class})
    public Map<String, Rules> centralDogmaRules(BeanResolver beanResolver, CentralDogma centralDogma, ObjectMapper objectMapper) throws Exception {
        log.info("load  centralDogmaRules");
        Map<String, Rules> rules = new HashMap<>();
        if (Objects.isNull(easyRules)) {
            CompletableFuture<Entry<String>> future =
                    centralDogma.getFile(properties.getProject(), properties.getRepo(), Revision.HEAD, Query.ofText(properties.getConfName()));
            log.info("load rule  content from centralDogma", future.join().content());
            List<RulesConfig> rulesConfigs = objectMapper.readValue(future.join().content(), new TypeReference<List<RulesConfig>>() {
            });
            easyRulesEntities.setRulesConfigs(rulesConfigs);
            return getStringRulesMap(beanResolver, rules);
        } else {
            log.info("load  centralDogmaRules from local watch cache");
            return this.easyRules;
        }

    }

    /**
     * common method for Map<String, Rules> convert
     * @param beanResolver
     * @param rules
     * @return
     */
    private Map<String, Rules> getStringRulesMap(BeanResolver beanResolver, Map<String, Rules> rules) {
        easyRulesEntities.getRulesConfigs().forEach(new Consumer<RulesConfig>() {
            @Override
            public void accept(RulesConfig rulesConfig) {
                log.info("load rule conf to easy rules engine:{}", rulesConfig.getRulesContent());
                StringReader stringReader = new StringReader(rulesConfig.getRulesContent().toPrettyString());
                SpELRuleFactory jsonRuleFactory = null;
                if(properties.isTemplate()){
                    log.info("use spel template context ");
                    jsonRuleFactory= new SpELRuleFactory(new JsonRuleDefinitionReader(), ParserContext.TEMPLATE_EXPRESSION, beanResolver);
                }else{
                    jsonRuleFactory= new SpELRuleFactory(new JsonRuleDefinitionReader(), beanResolver);
                }
                Rules jsonRules = null;
                try {
                    jsonRules = jsonRuleFactory.createRules(stringReader);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                rules.put(rulesConfig.getRulesId(), jsonRules);
            }
        });
        return rules;
    }

    /**
     * convert rulesContent2EasyRules
     *
     * @param beanResolver
     * @return
     */
    private Map<String, Rules> rulesContent2EasyRules(BeanResolver beanResolver) {
        Map<String, Rules> rules = new HashMap<>();
        return getStringRulesMap(beanResolver, rules);
    }
    /**
     * 获取配置额规则列表
     *
     * @param beanResolver spring beanResolver
     * @return Map<String, Rules>
     * @throws Exception
     */
    /**
     * 为了安全使用原型模式
     *
     * @param defaultRulesListener
     * @param defaultRuleEngineListener
     * @return RulesEngine
     */
    @Bean
    @ConditionalOnMissingBean(RulesEngine.class)
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE,proxyMode = ScopedProxyMode.TARGET_CLASS)
    public RulesEngine rulesEngine(RuleListener defaultRulesListener, RulesEngineListener defaultRuleEngineListener) {
        log.info("create rule Engine");
        RulesEngineParameters parameters = new RulesEngineParameters();
        if (this.properties.getPriorityThreshold() > 0) {
            parameters.setPriorityThreshold(this.properties.getPriorityThreshold());
        }
        if (this.properties.isSkipOnFirstAppliedRule()) {
            parameters.setSkipOnFirstAppliedRule(this.properties.isSkipOnFirstAppliedRule());
        }
        if (this.properties.isSkipOnFirstFailedRule()) {
            parameters.setSkipOnFirstFailedRule(this.properties.isSkipOnFirstFailedRule());
        }
        if (this.properties.isSkipOnFirstNonTriggeredRule()) {
            parameters.setSkipOnFirstNonTriggeredRule(this.properties.isSkipOnFirstNonTriggeredRule());
        }
        DefaultRulesEngine rulesEngine = new DefaultRulesEngine(parameters);
        rulesEngine.registerRuleListener(defaultRulesListener);
        rulesEngine.registerRulesEngineListener(defaultRuleEngineListener);
        return rulesEngine;
    }

}
