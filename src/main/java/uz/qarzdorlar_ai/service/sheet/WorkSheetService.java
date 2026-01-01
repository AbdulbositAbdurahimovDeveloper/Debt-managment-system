package uz.qarzdorlar_ai.service.sheet;

import java.util.List;

public interface WorkSheetService {
    void appendRow(String sheetName, List<Object> rowData);

    List<List<Object>> readRange(String range);

    void updateCell(String cellAddress, Object value);
}
