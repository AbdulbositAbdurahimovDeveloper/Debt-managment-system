package uz.qarzdorlar_ai.healthInfoBot;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.qarzdorlar_ai.controller.bot.UpdateDispatcherService;

@Slf4j
@Getter
@Component
@RequiredArgsConstructor
public class HealthBot extends TelegramWebhookBot {

    private final UpdateDispatcherService updateDispatcherService;

    @Value("${telegram.health.username}")
    private String botUsername;

    @Value("${telegram.health.token}")
    private String botToken;

    @Value("${telegram.health.webhook-path}")
    private String botPath;

    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        updateDispatcherService.dispatch(update); // This method should be marked @Async
        return null;
    }

    public void healthExecute(SendMessage sendMessage){
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}