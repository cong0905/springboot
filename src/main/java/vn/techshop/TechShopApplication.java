package vn.techshop;

import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@org.springframework.scheduling.annotation.EnableScheduling
@org.springframework.scheduling.annotation.EnableAsync
@SpringBootApplication
public class TechShopApplication {
    public static void main(String[] args) { SpringApplication.run(TechShopApplication.class, args); }
    @Bean Clock clock() { return Clock.systemUTC(); }
}
