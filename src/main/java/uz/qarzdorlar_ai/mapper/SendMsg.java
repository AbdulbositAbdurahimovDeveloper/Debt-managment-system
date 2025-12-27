package uz.qarzdorlar_ai.mapper;

import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.*;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.*;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;

public interface SendMsg {

    SendMessage sendMessage(Long chatId, String message);

    SendMessage sendMessage(String chatId, String message);

    SendMessage sendMessage(Long chatId, String message, ReplyKeyboard keyboard);

    DeleteMessage deleteMessage(Long chatId, Integer messageId);

    SendMessage sendReplyKeyboardRemove(Long chatId, String text);

    EditMessageText editMessage(Long chatId, Integer messageId, String menuText, InlineKeyboardMarkup keyboard);

    EditMessageText editMessage(Long chatId, Integer messageId, String menuText);

    EditMessageReplyMarkup editMarkup(Long chatId, Integer messageId);

    /**
     * Xabardagi rasmni, sarlavhani (caption) va tugmalarni bir vaqtda tahrirlaydi.
     *
     * @param chatId    Kimga yuborilishi
     * @param messageId Qaysi xabar tahrirlanishi
     * @param fileId    Rasmning telegramdagi file_id si
     * @param caption   Rasm ostidagi matn
     * @param keyboard  Yangi inline tugmalar
     * @return Tayyor EditMessageMedia obyekti
     */
    EditMessageMedia editMessageMedia(Long chatId, Integer messageId, String fileId, String caption, InlineKeyboardMarkup keyboard);

    /**
     * Rasm, sarlavha va tugmalar bilan xabar yuboradi.
     *
     * @param chatId   Kimga yuborilishi
     * @param file     Rasm (InputFile)
     * @param caption  Rasm ostidagi matn
     * @param keyboard Inline tugmalar
     * @return Tayyor SendPhoto obyekti
     */
    SendPhoto sendPhoto(Long chatId, String file, String caption, InlineKeyboardMarkup keyboard);


    SendPhoto sendPhoto(String channelId, InputFile file);

    SendPhoto sendPhoto(Long chatId, String file);

    SendPhoto sendPhoto(Long chatId, String fileId, String caption);

    AnswerCallbackQuery answerCallbackQuery(String callbackQueryId, String text);

    SendVideo sendVideo(Long chatId, String telegramFileId,InlineKeyboardMarkup keyboard);

    EditMessageCaption editMessageCaption(Long chatId, Integer messageId, String caption, InlineKeyboardMarkup keyboard);


    SendPhoto sendPhoto(String channelId, String fileId, String caption);

    SendVideo sendVideo(String channelId, String fileId, String caption);

    SendAudio sendAudio(String channelId, String fileId, String caption);

    SendVoice sendVoice(String channelId, String fileId, String caption);

    SendVideoNote sendVideoNote(String channelId, String fileId);

}
