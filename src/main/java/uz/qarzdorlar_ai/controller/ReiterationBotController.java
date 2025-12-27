package uz.qarzdorlar_ai.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import uz.qarzdorlar_ai.payload.WebhookRequest;

@Slf4j
@RestController
@RequestMapping("/api/register")
@RequiredArgsConstructor
public class ReiterationBotController {

    @Value("${telegram.bot.secret-token}")
    private String secretKey;

    @Value("${telegram.bot.token}")
    private String botToken;

    private final RestTemplate restTemplate;

    @PostMapping("/bot")
    public ResponseEntity<?> register(@RequestBody WebhookRequest request) {
        log.info("Kelgan so'rov: {}", request.getDomain());
        try {
            String result = registerWebhook(request.getDomain());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Xatolik: " + e.getMessage());
        }
    }

    public String registerWebhook(String domain) {
        if (domain == null || !domain.startsWith("https://")) {
            throw new IllegalArgumentException("Domain faqat HTTPS bilan boshlanishi kerak!");
        }

        // Domain oxiridagi "/" ni tekshirish
        if (domain.endsWith("/")) domain = domain.substring(0, domain.length() - 1);

        // PATHNI TO'G'RI KO'RSATISH: /api/telegram/update
        String telegramUrl = String.format(
                "https://api.telegram.org/bot%s/setWebhook?url=%s/telegram-bot&secret_token=%s",
                botToken, domain, secretKey
        );

        try {
            log.info("Telegram Webhook ro'yxatdan o'tkazilmoqda: {}", domain);
            return restTemplate.getForObject(telegramUrl, String.class);
        } catch (Exception e) {
            log.error("Webhook o'rnatishda xatolik: {}", e.getMessage());
            return "Xatolik: " + e.getMessage();
        }
    }

}
