package uz.qarzdorlar_ai.service.sheet;

import uz.qarzdorlar_ai.payload.sheet.WorkJournalDTO;

import java.util.List;

public interface WorkSheetService {
    void appendRow(String sheetName, List<Object> rowData);

    List<List<Object>> readRange(String range);

    void updateCell(String cellAddress, Object value);

    List<WorkJournalDTO> importGoogleSheetData();

    List<WorkJournalDTO> findByTypeAndSupplier(String targetType, String targetSupplier);
}
