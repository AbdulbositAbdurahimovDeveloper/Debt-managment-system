package uz.qarzdorlar_ai.service.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import uz.qarzdorlar_ai.Utils;
import uz.qarzdorlar_ai.payload.GoogleSheetData;
import uz.qarzdorlar_ai.payload.ProductParseDTO;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final ChatModel chatModel;

    @Override
    public List<ProductParseDTO> productParseAI(List<GoogleSheetData> googleSheetDataList) {
        if (googleSheetDataList == null || googleSheetDataList.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            // 1. Output Converter: Javobni List<ProductParseDTO> ga o'girish uchun
            BeanOutputConverter<List<ProductParseDTO>> converter =
                    new BeanOutputConverter<>(new ParameterizedTypeReference<List<ProductParseDTO>>() {
                    });

            // 2. Ma'lumotni AIga tushunarli formatda tayyorlash
            String itemsToProcess = googleSheetDataList.stream()
                    .map(data -> String.format("ProductSource: %s | PriceSource: %s", data.getItem(), data.getPrice()))
                    .collect(Collectors.joining("\n"));

            // Agar PromptTemplate xatolik berishda davom etsa, muqobil yo'l:
            String finalPrompt = Utils.Prompts.userPrompt
                    .replace("{items}", itemsToProcess)
                    .replace("{format}", converter.getFormat());

            Prompt prompt = new Prompt(finalPrompt);

            // 3. Professional Prompt (Resolution va boshqa fieldlar uchun qat'iy ko'rsatmalar)
//            String userPrompt = Utils.Prompts.userPrompt;
//
//            PromptTemplate promptTemplate = new PromptTemplate(userPrompt);
//            Prompt prompt = promptTemplate.create(Map.of(
//                    "items", itemsToProcess,
//                    "format", converter.getFormat()
//            ));

            log.info("Requesting AI to parse {} products...", googleSheetDataList.size());

            // 4. AI Call
            String response = chatModel.call(prompt).getResult().getOutput().getText();

            // 5. Senior Approach: JSONni tozalash (AI ba'zan ```json qo'shib yuboradi)
            String cleanedResponse = cleanResponse(response);

            return converter.convert(cleanedResponse);

        } catch (Exception e) {
            log.error("AI processing error: ", e);
            return Collections.emptyList();
        }
    }

    /**
     * AI ba'zan javobni Markdown (```json ... ```) ichiga o'rab beradi.
     * Bu metod o'sha ortiqcha belgilarni tozalaydi.
     */
    private String cleanResponse(String response) {
        if (response == null) return "[]";
        String result = response.trim();
        if (result.startsWith("```json")) {
            result = result.substring(7, result.length() - 3).trim();
        } else if (result.startsWith("```")) {
            result = result.substring(3, result.length() - 3).trim();
        }
        return result;
    }
}