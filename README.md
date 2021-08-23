#  包装easy-rules 基于centraldogma 存储规则配置的spring boot starter

> 当前主要包装spel，对于mvel 后期添加，对于rules配置文件格式支持json&&yaml

## 使用说明

*  添加依赖

>  当前还没有发布公共仓库，暂时需要自己构建 支持easy-rules 4.1

```maven
<dependency>
    <groupId>com.github.rongfengliang</groupId>
    <artifactId>easy-rules-centraldogma-spring-boot-starter</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

* 参考demo

```code
 @RequestMapping(value = "/myrule", method = RequestMethod.POST)
    public  Object info(@RequestBody User user) throws Exception {
        SpringBeanUtil.centralDogmaRules().forEach(new BiConsumer<String, Rules>() {
            @Override
            public void accept(String s, Rules rules) {
                System.out.println(s);
                rules.forEach(new Consumer<Rule>() {
                    @Override
                    public void accept(Rule rule) {
                        System.out.println(rule.getDescription());
                    }
                });
            }
        });
        Rules rules =  SpringBeanUtil.centralDogmaRules().get("demoapp");
        Facts facts = new Facts();
        // 生成一个唯一id，方便基于数据id规则流程查询
        user.setUniqueId(UUID.randomUUID().toString());
        facts.put("biz",user);
        SpringBeanUtil.getBean("rulesEngine", RulesEngine.class).fire(rules,facts);
        User userResult=  facts.get("biz");
        System.out.println("result from final ruls"+userResult.toString());
        return userResult;
    }
```

* 配置说明

添加规则配置

src/main/resources/application.yaml

```yaml
easyrules:
  skipOnFirstAppliedRule: false
  skipOnFirstNonTriggeredRule: false
  priorityThreshold: 1000000
  project: demo
  repo: demo
  contentType: json // 当前只支持json格式的，后续扩展其他的
  confName: /rules2.json
  template: false // 是否使用spel 模版格式为 #{},默认为false，为true 需要使用#{} 格式
centraldogma:
  hosts:
    - "127.0.0.1:36462"
server:
  port: 9000
```


添加规则文件: /rules2.json 在centraldogma中

1.0-SNAPSHOT starter 参考配置 

> 当前配置只支持基于spel的，后续会添加其他格式的



`template:false` 使用如下格式:

```json
[
  {
    "rulesId": "demoapp",
    "rulesContent": [
      {
        "name": "1",
        "description": "1ssssss",
        "priority": 1,
        "compositeRuleType": "UnitRuleGroup",
        "composingRules": [
          {
            "name": "2",
            "description": "2",
            "condition": "#biz.age >18",
            "priority": 2,
            "actions": [
              "@myService.setInfo(#biz)"
            ]
          }
        ]
      }
    ]
  },
  {
    "rulesId": "demoapp222",
    "rulesContent": [
      {
        "name": "1",
        "description": "1ssssss",
        "priority": 1,
        "compositeRuleType": "UnitRuleGroup",
        "composingRules": [
          {
            "name": "2",
            "description": "2",
            "condition": "#biz.age >18",
            "priority": 2,
            "actions": [
              "@myService.setInfo(#biz)"
            ]
          }
        ]
      }
    ]
  },
  {
    "rulesId": "demoapp2223333",
    "rulesContent": [
      {
        "name": "1",
        "description": "1ssssss",
        "priority": 1,
        "compositeRuleType": "UnitRuleGroup",
        "composingRules": [
          {
            "name": "2",
            "description": "2",
            "condition": "#biz.age >18",
            "priority": 2,
            "actions": [
              "@myService.setInfo(#biz)"
            ]
          }
        ]
      }
    ]
  }
]

```

`template:true` 使用如下格式：

```json
[
  {
    "rulesId": "demoapp",
    "rulesContent": [
      {
        "name": "demo",
        "description": "mydemo",
        "priority": 1,
        "compositeRuleType": "UnitRuleGroup",
        "composingRules": [
          {
            "name": "first",
            "description": "first rule",
            "condition": "#{#biz.age > 10}",
            "priority": 2,
            "actions": [
              "#{@myService.setInfo(#biz)}",
              "#{@myService.setInfo2(#biz)}"
            ]
          }
        ]
      }
    ]
  }
]

```

## 代码使用说明

* 基于pojo 代码的rule 添加到规则链中

是直接支持的，使用方法如下:

定义rule
```code
@Rule(priority = 0,description = "demo")
public class MyRule {
    @Condition
    public boolean itRains(@Fact("biz") User user) {
        System.out.println(user.toString());
        return true;
    }
    @Action
    public void takeAnUmbrella(@Fact("biz") User user) {
        System.out.println("run from first");
        System.out.println(user.toString());
    }
}
```

添加rule到规则链

```code
  ...
  Rules rules =  SpringBeanUtil.centralDogmaRules().get("demoapp");
  ...
  rules.register(new MyRule());
  ...
  
  // do rule fire and then fetch result
  
```
 
## 几个扩展点

* rulelistener

可以添加自己的bean，方便记录信息，比如分析业务规则的执行，每个阶段的数据，计划支持prometheus的metrics
以及rule pipeline

参考实现:
```code
@Component
public class MyRuleListener implements RuleListener {
    Log log = LogFactory.getLog(DefaultRulesListener.class);
    @Override
    public boolean beforeEvaluate(Rule rule, Facts facts) {
        return true;
    }

    @Override
    public void afterEvaluate(Rule rule, Facts facts, boolean b) {
        log.info("-----------------afterEvaluate-----------------");
        log.info("my RulesListener: "+"rule name: "+rule.getName()+"rule desc: "+rule.getDescription()+facts.toString());
    }

    @Override
    public void beforeExecute(Rule rule, Facts facts) {
        log.info("-----------------beforeExecute-----------------");
        log.info("my RulesListener: "+"rule name: "+rule.getName()+"rule desc: "+rule.getDescription()+facts.toString());
    }

    @Override
    public void onSuccess(Rule rule, Facts facts) {
        log.info("-----------------onSuccess-----------------");
        log.info("my RulesListener: "+"rule name: "+rule.getName()+"rule desc: "+rule.getDescription()+facts.toString());
    }

    @Override
    public void onFailure(Rule rule, Facts facts, Exception e) {
        log.info("-----------------onFailure-----------------");
        log.info("my RulesListener: "+"rule name: "+rule.getName()+"rule desc: "+rule.getDescription()+facts.toString());
    }
}

```
* ruleEnginelistener

可以添加自己的bean，方便记录信息，方便分析rulegine的情况计划