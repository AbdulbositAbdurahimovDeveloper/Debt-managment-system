package uz.qarzdorlar_ai.service.sheet;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uz.qarzdorlar_ai.payload.sheet.WorkJournalDTO;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class WorkSheetServiceImpl implements WorkSheetService {

    private Sheets sheetsService;

    @Value("${google.sheets.application-name}")
    private String applicationName;

    @Value("${google.sheets.credentials-path}")
    private String credentialsPath;

    @Value("${google.sheets.spreadsheet-work-id}")
    private String spreadsheetId;

    private static final String VALUE_INPUT_OPTION = "USER_ENTERED";
    private static final String INSERT_DATA_OPTION = "INSERT_ROWS";
    private static final String UPDATED_HEADER_PREFIX = "Data updated date";
    private static final String DATE_TIME_FORMAT = "dd.MM.yyyy 'time' HH:mm:ss";

    @PostConstruct
    public void init() {
        try {
            log.info("Initializing Google Sheets API connection...");

            InputStream in = getClass().getResourceAsStream(credentialsPath);
            if (in == null) {
                log.error("Credentials file not found at path: {}", credentialsPath);
                throw new RuntimeException("Google credentials file is missing.");
            }

            GoogleCredentials credentials = GoogleCredentials.fromStream(in)
                    .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));

            this.sheetsService = new Sheets.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials))
                    .setApplicationName(applicationName)
                    .build();

            log.info("Google Sheets API initialized successfully.");

        } catch (IOException | GeneralSecurityException e) {
            log.error("Failed to initialize Google Sheets service: ", e);
            throw new RuntimeException("Critical failure during Google Sheets initialization", e);
        }
    }

    @Override
    public void appendRow(String sheetName, List<Object> rowData) {
        Objects.requireNonNull(sheetName, "Sheet name must not be null");
        Objects.requireNonNull(rowData, "Row data must not be null");

        try {
            ValueRange body = new ValueRange().setValues(Collections.singletonList(rowData));

            AppendValuesResponse result = sheetsService.spreadsheets().values()
                    .append(spreadsheetId, sheetName, body)
                    .setValueInputOption(VALUE_INPUT_OPTION)
                    .setInsertDataOption(INSERT_DATA_OPTION)
                    .execute();

            log.debug("Successfully appended row to {}. Updated cells: {}", sheetName, result.getUpdates().getUpdatedCells());
        } catch (IOException e) {
            log.error("Error appending row to sheet {}: ", sheetName, e);
            throw new RuntimeException("Data persistence failed in Google Sheets", e);
        }
    }

    @Override
    public List<List<Object>> readRange(String range) {
        Objects.requireNonNull(range, "Range must not be null (e.g., 'Sheet1!A1:E10')");

        try {
            ValueRange response = sheetsService.spreadsheets().values()
                    .get(spreadsheetId, range)
                    .execute();

            List<List<Object>> values = response.getValues();
            if (values == null || values.isEmpty()) {
                log.warn("No data found for range: {}", range);
                return Collections.emptyList();
            }

            return values;
        } catch (IOException e) {
            log.error("Error reading range {}: ", range, e);
            throw new RuntimeException("Data retrieval failed from Google Sheets", e);
        }
    }

    @Override
    public void updateCell(String cellAddress, Object value) {
        Objects.requireNonNull(cellAddress, "Cell address must not be null (e.g., 'Sheet1!A1')");

        try {
            ValueRange body = new ValueRange()
                    .setValues(Collections.singletonList(Collections.singletonList(value)));

            UpdateValuesResponse result = sheetsService.spreadsheets().values()
                    .update(spreadsheetId, cellAddress, body)
                    .setValueInputOption(VALUE_INPUT_OPTION)
                    .execute();

            log.debug("Successfully updated cell {}. Updated cells: {}", cellAddress, result.getUpdatedCells());
        } catch (IOException e) {
            log.error("Error updating cell {}: ", cellAddress, e);
            throw new RuntimeException("Cell update failed in Google Sheets", e);
        }
    }

    @Override
    public List<WorkJournalDTO> importGoogleSheetData() {
        String sheetName = "Jurnal";
        // B5:Q128 oralig'ini o'qiymiz
        String range = sheetName + "!B5:Q128";

        List<List<Object>> values = readRange(range);
        List<WorkJournalDTO> dtos = new ArrayList<>();

        if (values == null || values.isEmpty()) {
            return dtos;
        }

        // Agar 5-qator (index 0) header bo'lsa, i=1 dan boshlaymiz (ma'lumot 6-qatordan boshlanadi)
        // Agar 5-qatordan ma'lumot boshlansa, i=0 dan boshlang.
        for (int i = 1; i < values.size(); i++) {
            List<Object> row = values.get(i);

            // Qator butunlay bo'sh bo'lsa tashlab ketamiz
            if (row == null || row.isEmpty()) {
                continue;
            }

            WorkJournalDTO dto = new WorkJournalDTO();

            // getNullSafeValue metodi bo'sh ("") qiymat kelsa null qo'yib ketadi
            dto.setDate(getNullSafeValue(row, 1));      // C
            dto.setType(getNullSafeValue(row, 2));      // D
            dto.setCourier(getNullSafeValue(row, 3));   // E
            dto.setSupplier(getNullSafeValue(row, 4));  // F
            dto.setItem(getNullSafeValue(row, 5));      // G
            dto.setCount(getNullSafeValue(row, 6));     // H
            dto.setPrice(getNullSafeValue(row, 7));     // I
            dto.setDebtAed(getNullSafeValue(row, 8));   // J
            dto.setPaidAed(getNullSafeValue(row, 9));   // K
            dto.setCashUsd(getNullSafeValue(row, 10));  // L
            dto.setRate(getNullSafeValue(row, 11));     // M
            dto.setFee(getNullSafeValue(row, 12));      // N
            dto.setUsedUsd(getNullSafeValue(row, 13));  // O
            dto.setComment(getNullSafeValue(row, 14));  // P
            dto.setProductId(getNullSafeValue(row, 15));// Q

            dtos.add(dto);
        }

        return dtos;
    }
    @Override
    public List<WorkJournalDTO> findByTypeAndSupplier(String targetType, String targetSupplier) {
        String sheetName = "Jurnal";
        // B5 dan Q128 gacha o'qiymiz
        String range = sheetName + "!B5:Q128";

        List<List<Object>> values = readRange(range);
        List<WorkJournalDTO> dtos = new ArrayList<>();

        if (values == null || values.isEmpty()) {
            return dtos;
        }

        for (List<Object> row : values) {
            if (row == null || row.isEmpty()) continue;

            // D ustuni (index 2) - Type
            String rowType = getNullSafeValue(row, 2);
            // F ustuni (index 4) - Supplier
            String rowSupplier = getNullSafeValue(row, 3);

            // Filtrlash sharti: Katta-kichik harfga qaramasdan solishtiramiz
//            boolean matchesType = (targetType == null) || (rowType != null && rowType.equalsIgnoreCase(targetType));
            boolean matchesType = true;
            boolean matchesSupplier = (targetSupplier == null) || (rowSupplier != null && rowSupplier.equalsIgnoreCase(targetSupplier));

            if (matchesType && matchesSupplier) {
                // Agar mos kelsa, DTO obyektini yaratamiz
                dtos.add(mapToDto(row));
            }
        }

        return dtos;
    }

    /**
     * Qatorni DTO ga o'girish metodini alohida yozish kodni toza saqlaydi
     */
    private WorkJournalDTO mapToDto(List<Object> row) {
        WorkJournalDTO dto = new WorkJournalDTO();

        // Indexlar B5 oralig'iga nisbatan (B=0, C=1, D=2...)
        dto.setDate(getNullSafeValue(row, 1));      // C
        dto.setType(getNullSafeValue(row, 2));      // D
        dto.setCourier(getNullSafeValue(row, 3));   // E
        dto.setSupplier(getNullSafeValue(row, 4));  // F
        dto.setItem(getNullSafeValue(row, 5));      // G
        dto.setCount(getNullSafeValue(row, 6));     // H
        dto.setPrice(getNullSafeValue(row, 7));     // I
        dto.setDebtAed(getNullSafeValue(row, 8));   // J
        dto.setPaidAed(getNullSafeValue(row, 9));   // K
        dto.setCashUsd(getNullSafeValue(row, 10));  // L
        dto.setRate(getNullSafeValue(row, 11));     // M
        dto.setFee(getNullSafeValue(row, 12));      // N
        dto.setUsedUsd(getNullSafeValue(row, 13));  // O
        dto.setComment(getNullSafeValue(row, 14));  // P

        // Product ID (Q ustuni - index 15)
        String productIdStr = getNullSafeValue(row, 15);
        if (productIdStr != null) {
            try {
                dto.setProductId(productIdStr);
            } catch (NumberFormatException e) {
                log.warn("Product ID formati noto'g'ri: {}", productIdStr);
            }
        }

        return dto;
    }

    /**
     * Google Sheet'dan kelgan qiymatni tekshiradi.
     * Agar index mavjud bo'lmasa yoki qiymat bo'sh ("") bo'lsa, null qaytaradi.
     */
    private String getNullSafeValue(List<Object> row, int index) {
        if (row == null || index >= row.size() || row.get(index) == null) {
            return null;
        }

        String value = String.valueOf(row.get(index)).trim();

        // Agar string bo'sh bo'lsa null qaytaramiz
        return value.isEmpty() ? null : value;
    }


    // Yordamchi metod: Indexdan ma'lumotni xavfsiz olish uchun
    private String getCellValue(List<Object> row, int index) {
        if (index >= row.size() || row.get(index) == null) {
            return "";
        }
        return String.valueOf(row.get(index)).trim();
    }

}
