package br.com.marazulturismo.marazulbackendadmin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MarazulBackEndAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(MarazulBackEndAdminApplication.class, args);
    }

}
