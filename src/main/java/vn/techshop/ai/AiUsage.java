package vn.techshop.ai;

import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import vn.techshop.shared.BusinessException;
import org.springframework.http.HttpStatus;

@Service
public class AiUsage {
    private final JdbcTemplate jdbc;private final Clock clock;private final AiSettings settings;
    public AiUsage(JdbcTemplate jdbc,Clock clock,AiSettings settings){this.jdbc=jdbc;this.clock=clock;this.settings=settings;}
    private record Limit(String key,int maximum){}
    @Transactional public boolean tryTurn(Long user){
        long minute=clock.instant().getEpochSecond()/60;String day=LocalDate.now(clock.withZone(ZoneId.of("Asia/Ho_Chi_Minh"))).toString();
        return consume(List.of(new Limit("user:"+user+":m:"+minute,settings.number("user-turns-per-minute",2,30)),new Limit("user:"+user+":d:"+day,settings.number("user-turns-per-day",20,1000))),false);
    }
    @Transactional(propagation=Propagation.REQUIRES_NEW) public void request(){
        long minute=clock.instant().getEpochSecond()/60;String day=LocalDate.now(clock.withZone(ZoneId.of("Asia/Ho_Chi_Minh"))).toString();
        consume(List.of(new Limit("global:m:"+minute,settings.number("global-requests-per-minute",10,1000)),new Limit("global:d:"+day,settings.number("global-requests-per-day",200,100000))),true);
    }
    private boolean consume(List<Limit> limits,boolean fail){
        jdbc.queryForObject("select id from ai_usage_gate where id=1 for update",Integer.class);
        for(var limit:limits){Integer count=jdbc.query("select usage_count from ai_usage_limits where usage_key=?",r->r.next()?r.getInt(1):0,limit.key());
            if(count>=limit.maximum()){if(!fail)return false;throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS,"AI_QUOTA_EXCEEDED","Bạn đã dùng hết lượt AI cho khoảng thời gian này. Hãy chọn gặp nhân viên.");}}
        for(var limit:limits){int updated=jdbc.update("update ai_usage_limits set usage_count=usage_count+1 where usage_key=?",limit.key());if(updated==0)jdbc.update("insert into ai_usage_limits(usage_key,usage_count,created_at) values (?,1,?)",limit.key(),java.sql.Timestamp.from(clock.instant()));}
        return true;
    }
    @Transactional(readOnly=true) public Map<String,Object> status(){
        String day=LocalDate.now(clock.withZone(ZoneId.of("Asia/Ho_Chi_Minh"))).toString();Integer used=jdbc.query("select usage_count from ai_usage_limits where usage_key=?",r->r.next()?r.getInt(1):0,"global:d:"+day);
        return Map.of("enabled",settings.enabled(),"configured",settings.configured(),"available",settings.ready(),"model",settings.model(),"requestsToday",used,"dailyRequestLimit",settings.number("global-requests-per-day",200,100000));
    }
}
