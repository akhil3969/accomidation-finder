package com.accommodationfinder.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger UI is served at /swagger-ui.html with a working "Authorize" button. */
@Configuration
public class OpenApiConfig {

    private static final String SCHEME = "bearerAuth";

    @Bean
    public OpenAPI accommodationFinderOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Accommodation Finder API")
                        .version("1.0.0")
                        .description("Real-time accommodation search, booking and messaging API.")
                        .contact(new Contact().name("Kona Sai Akhil").url("https://github.com/akhil3969"))
                        .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME))
                .components(new Components().addSecuritySchemes(SCHEME,
                        new SecurityScheme()
                                .name(SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
