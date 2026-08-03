package kr.co.firstdayproject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FirstDayProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(FirstDayProjectApplication.class, args);
    }

}
