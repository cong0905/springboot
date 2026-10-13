package vn.techshop.ai;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class AiSettings {
    private final Environment env;
    public AiSettings(Environment env){this.env=env;}
    public boolean enabled(){return env.getProperty("app.ai.enabled",Boolean.class,false);}
    public String key(){return env.getProperty("app.ai.api-key","").strip();}
    public String model(){return env.getProperty("app.ai.model","").strip();}
    public boolean configured(){return !key().isEmpty() && model().matches("[a-zA-Z0-9._-]{1,100}");}
    public boolean ready(){return enabled() && configured();}
    public boolean workerEnabled(){return env.getProperty("app.ai.worker-enabled",Boolean.class,true);}
    public int number(String name,int fallback,int maximum){return Math.max(1,Math.min(maximum,env.getProperty("app.ai."+name,Integer.class,fallback)));}
    public int timeoutSeconds(){return number("turn-timeout-seconds",30,60);}
    public int concurrency(){return number("max-concurrent-calls",2,4);}
    @Override public String toString(){return "AiSettings[enabled="+enabled()+",configured="+configured()+"]";}
}
