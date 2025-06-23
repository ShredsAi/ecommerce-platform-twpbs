package ai.shreds;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WebhookReconciliationShredApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebhookReconciliationShredApplication.class, args);
    }
}
