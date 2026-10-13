package vn.techshop.identity;
import java.time.Clock;
import java.time.Duration;
import java.util.*;
import org.springframework.stereotype.Component;
import vn.techshop.shared.BusinessException;
@Component
public class RequestLimits {
 private record Window(long until,int count){}
 private final Clock clock;private final Map<String,Window> windows=new HashMap<>();
 public RequestLimits(Clock clock){this.clock=clock;}
 public synchronized void consume(String key,int max,Duration duration){
  long now=clock.millis();windows.entrySet().removeIf(e->e.getValue().until()<=now);
  var old=windows.get(key);if(old==null&&windows.size()>=10000||old!=null&&old.count()>=max)throw new BusinessException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS,"TOO_MANY_ATTEMPTS","Bạn đã gửi quá nhiều yêu cầu. Hãy thử lại sau.");
  windows.put(key,new Window(old==null?now+duration.toMillis():old.until(),old==null?1:old.count()+1));
 }
}
