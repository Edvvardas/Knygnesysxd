package lt.prifkodas.knygnesys;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@EnableJpaRepositories
@SpringBootApplication
@EnableScheduling
public class KnygnesysApplication {

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    public static void main(String[] args) {
        SpringApplication.run(KnygnesysApplication.class, args);
    }

}
