package lt.prifkodas.knygnesys.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI knygnesysOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Knygnesys API")
                        .description("Knygų skaitymo, vertinimo ir dalinimosi sistema")
                        .version("1.0.0"));
    }
}
