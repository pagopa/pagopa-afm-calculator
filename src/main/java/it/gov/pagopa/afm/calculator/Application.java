package it.gov.pagopa.afm.calculator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
//@EnableCaching TODO remember to enable the cache!
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
