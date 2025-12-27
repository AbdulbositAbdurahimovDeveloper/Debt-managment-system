package uz.qarzdorlar_ai.config.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.ChatMemberUpdated;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

@Service
public class TelegramUpdateLogger {


    private static final Logger log = LoggerFactory.getLogger(TelegramUpdateLogger.class);


    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_BLUE = "\u001B[34m";

    /**
     * Telegramdan kelgan har bir Update'ni chiroyli formatda loglaydi.
     *
     * @param update Telegramdan kelgan asosiy obyekt.
     */
    public void logUpdate(Update update) {
        StringBuilder logBuilder = new StringBuilder();


        logBuilder.append("\n")
                .append(ANSI_GREEN).append("================ INCOMING TELEGRAM REQUEST ================").append(ANSI_RESET)
                .append("\n");

        if (update.hasMessage() && update.getMessage().hasText()) {
            User from = update.getMessage().getFrom();
            String text = update.getMessage().getText();

            logBuilder.append(ANSI_CYAN).append("Type      : ").append(ANSI_RESET).append("Text Message\n");
            logBuilder.append(ANSI_YELLOW).append("User      : ").append(ANSI_RESET)
                    .append(from.getFirstName()).append(" (ID: ").append(from.getId()).append(", Username: @")
                    .append(from.getUserName()).append(")\n");
            logBuilder.append(ANSI_YELLOW).append("Chat ID   : ").append(ANSI_RESET).append(update.getMessage().getChatId()).append("\n");
            logBuilder.append(ANSI_YELLOW).append("Content   : ").append(ANSI_RESET).append("\"").append(text).append("\"\n");

        } else if (update.hasCallbackQuery()) {
            User from = update.getCallbackQuery().getFrom();
            String data = update.getCallbackQuery().getData();

            logBuilder.append(ANSI_CYAN).append("Type      : ").append(ANSI_RESET).append("Callback Query (Button Click)\n");
            logBuilder.append(ANSI_YELLOW).append("User      : ").append(ANSI_RESET)
                    .append(from.getFirstName()).append(" (ID: ").append(from.getId()).append(", Username: @")
                    .append(from.getUserName()).append(")\n");
            logBuilder.append(ANSI_YELLOW).append("Chat ID   : ").append(ANSI_RESET).append(update.getCallbackQuery().getMessage().getChatId()).append("\n");
            logBuilder.append(ANSI_YELLOW).append("Data      : ").append(ANSI_RESET).append("\"").append(data).append("\"\n");

        } else if (update.hasMyChatMember()) {
            ChatMemberUpdated chatMemberUpdated = update.getMyChatMember();
            User from = chatMemberUpdated.getFrom();
            String oldStatus = chatMemberUpdated.getOldChatMember().getStatus();
            String newStatus = chatMemberUpdated.getNewChatMember().getStatus();

            logBuilder.append(ANSI_CYAN).append("Type      : ").append(ANSI_RESET).append("Bot Status Change (my_chat_member)\n");
            logBuilder.append(ANSI_YELLOW).append("User      : ").append(ANSI_RESET)
                    .append(from.getFirstName()).append(" (ID: ").append(from.getId()).append(", Username: @")
                    .append(from.getUserName()).append(")\n");
            logBuilder.append(ANSI_YELLOW).append("Chat ID   : ").append(ANSI_RESET).append(chatMemberUpdated.getChat().getId()).append("\n");


            logBuilder.append(ANSI_BLUE).append("Status    : ").append(ANSI_RESET)
                    .append(oldStatus).append(" -> ");

            if (newStatus.equals("kicked")) {
                logBuilder.append(ANSI_RED).append(newStatus.toUpperCase()).append(ANSI_RESET).append(" (Bot Blocked)\n");
            } else if (newStatus.equals("member")) {
                logBuilder.append(ANSI_GREEN).append(newStatus.toUpperCase()).append(ANSI_RESET).append(" (Bot (Re)Started)\n");
            } else {
                logBuilder.append(newStatus).append("\n");
            }

        } else {

            logBuilder.append(ANSI_CYAN).append("Type      : ").append(ANSI_RESET).append("Unsupported or Unknown Update Type\n");
            logBuilder.append(ANSI_YELLOW).append("Update ID : ").append(ANSI_RESET).append(update.getUpdateId()).append("\n");
        }


        logBuilder.append(ANSI_GREEN).append("=========================================================").append(ANSI_RESET);


        log.info(logBuilder.toString());
    }
}