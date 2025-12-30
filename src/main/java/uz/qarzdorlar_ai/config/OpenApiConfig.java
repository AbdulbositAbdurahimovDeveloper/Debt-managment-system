package uz.qarzdorlar_ai.config;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;

@OpenAPIDefinition(
        info = @Info(
                title = "Debts-management-system",
                version = "${api.version}",
                contact = @Contact(
                        name = "not.uz",
                        email = "notuzcomputers@gmail.com",
                        url = "https://github.com/AbdulbositAbdurahimovDeveloper/Debt-managment-system.git"
                ),
                license = @License(
                        name = "Apache 2.0",
                        url = "https://springdoc.org"),
                termsOfService = "http://swagger.io/terms/",
                description = "Spring 6 Swagger Simple Application"
        ),
        externalDocs = @ExternalDocumentation(
                description = "Spring 6 Wiki Documentation", url = "https://springshop.wiki.github.org/docs"
        ),
        servers = {
                @Server(
                        url = "http://localhost:8080",
                        description = "Test-Server"
                ),
                @Server(
                        url = "http://213.199.55.47:9990",
                        description = "Production-Server"
                )

        },
        security = {
                @SecurityRequirement(name = "bearerAuth"),
                @SecurityRequirement(name = "basicAuth"),
        }
)

@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Authentication with JWT token writing."
)
//@SecurityScheme(
//        name = "basicAuth",
//        type = SecuritySchemeType.HTTP,
//        scheme = "basic"
//)
public class OpenApiConfig {
}
