package uz.qarzdorlar_ai.controller.bot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.qarzdorlar_ai.config.log.TelegramUpdateLogger;
import uz.qarzdorlar_ai.healthInfoBot.HealthBot;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TelegramBotController {

    private final DebtAiBot debtAiBot;
    private final HealthBot healthBot;
    private final TelegramUpdateLogger logger;

    @Value("${telegram.bot.secret-token}")
    private String expectedBotSecret;

    @Value("${telegram.health.secret-token}")
    private String expectedHealthSecret;

    @PostMapping("/telegram-bot")
    public BotApiMethod<?> onUpdateReceived(@RequestHeader("X-Telegram-Bot-Api-Secret-Token") String secret,
                                            @RequestBody Update update) {
        logger.logUpdate(update);

        if (!secret.equals(expectedBotSecret)) {
            return (BotApiMethod<?>) ResponseEntity.status(403);
        }
        return debtAiBot.onWebhookUpdateReceived(update);
    }

    @PostMapping("/health/telegram-bot")
    public BotApiMethod<?> onUpdateHealthReceived(@RequestHeader("X-Telegram-Bot-Api-Secret-Token") String secret,
                                                  @RequestBody Update update) {
        logger.logUpdate(update);

        if (!secret.equals(expectedHealthSecret)) {
            return (BotApiMethod<?>) ResponseEntity.status(403);
        }

        return healthBot.onWebhookUpdateReceived(update);
    }
}