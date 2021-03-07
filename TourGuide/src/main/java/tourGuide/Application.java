package tourGuide;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

import java.util.Locale;

@SpringBootApplication
@EnableEurekaClient
public class Application {

    public static void main(String[] args) {
        Locale.setDefault(new Locale("en", "US", "WIN"));
        SpringApplication.run(Application.class, args);
    }

}
