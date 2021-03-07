package fr.openclassrooms.rayane.gps;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@SpringBootApplication
@EnableEurekaClient
public class GpsClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(GpsClientApplication.class, args);
    }

}
