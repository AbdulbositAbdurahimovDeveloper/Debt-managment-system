package uz.qarzdorlar_ai.service.sheet;

import uz.qarzdorlar_ai.payload.sheet.GoogleSheetData;
import uz.qarzdorlar_ai.payload.sheet.GoogleSheetUsers;

import java.util.List;

public interface SheetService {

    List<GoogleSheetData> importGoogleSheetData();

    List<GoogleSheetUsers> importGoogleSheetUsers();

    void appendRow(String sheetName, List<Object> rowData);

    List<List<Object>> readRange(String range);

    void updateCell(String cellAddress, Object value);
}
