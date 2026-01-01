package uz.qarzdorlar_ai.service.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import uz.qarzdorlar_ai.Utils;
import uz.qarzdorlar_ai.payload.TransactionCreateDTO;
import uz.qarzdorlar_ai.payload.sheet.GoogleSheetData;
import uz.qarzdorlar_ai.payload.ProductParseDTO;
import uz.qarzdorlar_ai.payload.sheet.WorkJournalDTO;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    @Override
    public List<TransactionCreateDTO> transactionParseAI(List<WorkJournalDTO> journalDTOS) {
        if (journalDTOS == null || journalDTOS.isEmpty()) return Collections.emptyList();

        try {
            String itemsToProcess = journalDTOS.stream()
                    .map(dto -> String.format("ID:%s, Sana:%s, Tur:%s, Dastavchik:%s, Postavchik:%s, Item:%s, Soni:%s, Narxi:%s, Paid:%s, Cash:%s, Kurs:%s, ProdID:%s",
                            dto.getId(), dto.getDate(), dto.getType(), dto.getCourier(), dto.getSupplier(),
                            dto.getItem(), dto.getCount(), dto.getPrice(), dto.getPaidAed(), dto.getCashUsd(), dto.getRate(), dto.getProductId()))
                    .collect(Collectors.joining("\n"));

            String finalPrompt = """
            [SYSTEM INSTRUCTION: RETURN ONLY RAW JSON ARRAY. NO CONVERSATION. NO EXPLANATION.]
            
            Convert the following data into a JSON list of 'TransactionCreateDTO'.
            
            ### STRICT CLIENT IDS:
            2: 'Dilnoza opa', 3: 'Saxiy Ahmad', 4: 'Abduqodir'
            5: 'Khojandi Electronic', 6: 'Jawel', 7: 'Mohid Mirza', 8: 'Transworld', 9: 'Micro zone', 10: 'Ali Riza', 11: 'Preview'
            ID 1: 'Oybek aka' (If Dastavchik is 'Oybek aka' AND Tur is 'CASH-OUT', SKIP that row).
            
            ### TRANSACTION RULES:
            - Tur 'BUY' -> "PURCHASE"
            - Tur 'PAY' -> "PURCHASE_PAYMENT" (If Postavchik is 'Oybek aka' -> "RETURN_PAYMENT")
            - Tur 'CASH-OUT' -> If Cash > 0: "TRANSFER" (clientId=Dastavchik, receiverClientId=Postavchik). If Cash < 0: "RETURN_PAYMENT".
            - Tur 'RETURN' -> "RETURN_PAYMENT"
            
            ### JSON SPECIFICATION:
            - 'createdAt': Must be "2026-01-01T15:00:00.000Z".
            - 'marketRate' & 'clientRate': Must be >= 1.0 (NEVER 0).
            - 'type': ONLY ["PURCHASE", "PURCHASE_PAYMENT", "TRANSFER", "RETURN_PAYMENT"].
            
            ### DATA:
            %s
            
            [FINAL WARNING: OUTPUT MUST BE ONLY A VALID JSON ARRAY STARTING WITH '[' AND ENDING WITH ']'. DO NOT ADD ANY TEXT BEFORE OR AFTER.]
            """.formatted(itemsToProcess);

            String response = chatModel.call(new Prompt(finalPrompt)).getResult().getOutput().getText();

            // MUHIM: Kuchaytirilgan tozalash
            String cleaned = cleanResponsse(response);

            log.info("AI RAW Response (cleaned): {}", cleaned); // Logda ko'rish uchun

            return objectMapper.readValue(cleaned, new TypeReference<List<TransactionCreateDTO>>() {});

        } catch (Exception e) {
            log.error("AI Mapping Error at Row Batch: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private String cleanResponsse(String response) {
        if (response == null || response.isEmpty()) return "[]";

        // Matn ichidan birinchi [ va oxirgi ] ni topish
        int start = response.indexOf("[");
        int end = response.lastIndexOf("]");

        if (start != -1 && end != -1 && start < end) {
            return response.substring(start, end + 1);
        }

        // Agar [ ] topilmasa, markdown belgilarini tozalash (eski usul)
        String cleaned = response.replaceAll("```json", "").replaceAll("```", "").trim();
        return cleaned;
    }


//    // Yordamchi metod (oldiningizda bor bo'lsa shuni ishlating)
//    private String cleanResponse(String response) {
//        if (response == null) return "";
//        return response.replaceAll("```json", "").replaceAll("```", "").trim();
//    }

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