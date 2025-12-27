package uz.qarzdorlar_ai.service.ai;

import uz.qarzdorlar_ai.payload.GoogleSheetData;
import uz.qarzdorlar_ai.payload.ProductParseDTO;

import java.util.List;

public interface AiService {

    List<ProductParseDTO> productParseAI(List<GoogleSheetData> googleSheetDataList);
}
