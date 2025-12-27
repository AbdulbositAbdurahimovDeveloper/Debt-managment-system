package uz.qarzdorlar_ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ComponentScan("uz.qarzdorlar_ai")
public class WebMvcConfig implements WebMvcConfigurer {

    @Bean
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // Hamma endpointlar uchun
                .allowedOrigins("http://localhost:5173") // Faqat sening frontendingga ruxsat
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Ruxsat berilgan metodlar
                .allowedHeaders("*") // Hamma headerlarga ruxsat (masalan, Authorization)
                .allowCredentials(true); // Cookie yoki Tokenlar bilan ishlash uchun
    }

}