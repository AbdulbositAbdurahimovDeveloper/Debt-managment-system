package uz.qarzdorlar_ai.service.ai;

import uz.qarzdorlar_ai.payload.TransactionCreateDTO;
import uz.qarzdorlar_ai.payload.sheet.GoogleSheetData;
import uz.qarzdorlar_ai.payload.ProductParseDTO;
import uz.qarzdorlar_ai.payload.sheet.WorkJournalDTO;

import java.util.List;

public interface AiService {

    List<TransactionCreateDTO> transactionParseAI(List<WorkJournalDTO> journalDTOS);

    List<ProductParseDTO> productParseAI(List<GoogleSheetData> googleSheetDataList);
}
