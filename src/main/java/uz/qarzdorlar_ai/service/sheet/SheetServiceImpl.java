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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uz.qarzdorlar_ai.Utils;
import uz.qarzdorlar_ai.payload.GoogleSheetData;
import uz.qarzdorlar_ai.payload.GoogleSheetUsers;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class SheetServiceImpl implements SheetService {

    private Sheets sheetsService;

    @Value("${google.sheets.application-name}")
    private String applicationName;

    @Value("${google.sheets.credentials-path}")
    private String credentialsPath;

    @Value("${google.sheets.spreadsheet-id}")
    private String spreadsheetId;

    private static final String VALUE_INPUT_OPTION = "USER_ENTERED";
    private static final String INSERT_DATA_OPTION = "INSERT_ROWS";
    private static final String UPDATED_HEADER_PREFIX = "Data updated date";
    private static final String DATE_TIME_FORMAT = "dd.MM.yyyy 'time' HH:mm:ss";

    @PostConstruct
    public void init() {
//        try {
//            log.info("Initializing Google Sheets API connection...");
//
//            InputStream in = getClass().getResourceAsStream(credentialsPath);
//            if (in == null) {
//                log.error("Credentials file not found at path: {}", credentialsPath);
//                throw new RuntimeException("Google credentials file is missing.");
//            }
//
//            GoogleCredentials credentials = GoogleCredentials.fromStream(in)
//                    .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));
//
//            this.sheetsService = new Sheets.Builder(
//                    GoogleNetHttpTransport.newTrustedTransport(),
//                    GsonFactory.getDefaultInstance(),
//                    new HttpCredentialsAdapter(credentials))
//                    .setApplicationName(applicationName)
//                    .build();
//
//            log.info("Google Sheets API initialized successfully.");
//
//        } catch (IOException | GeneralSecurityException e) {
//            log.error("Failed to initialize Google Sheets service: ", e);
//            throw new RuntimeException("Critical failure during Google Sheets initialization", e);
//        }
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
    public List<GoogleSheetData> importGoogleSheetData() {
        String sheetName = Utils.Sheets.paper5;
        String range = sheetName + "!A:B"; // Columns A and B

        log.info("Fetching data from sheet range: {}", range);
        List<List<Object>> values = readRange(range);

        if (values == null || values.isEmpty()) {
            log.warn("Sheet is empty or could not be read.");
            return Collections.emptyList();
        }

        // 1. Check Row 1 (Index 0)
        List<Object> firstRow = values.get(0);
        if (firstRow.isEmpty() || !isValidHeader(firstRow.get(0).toString())) {
            log.info("Invalid header found in first cell. Skipping data processing.");
            return Collections.emptyList();
        }

        // 2. Process data from Row 2 (Index 1) onwards
        List<GoogleSheetData> resultList = new ArrayList<>();
        for (int i = 1; i < values.size(); i++) {
            List<Object> row = values.get(i);

            if (row.isEmpty()) continue;

            String item = (row.size() > 0 && row.get(0) != null) ? row.get(0).toString().trim() : "";
            String price = (row.size() > 1 && row.get(1) != null) ? row.get(1).toString().trim() : "";

            // Skip if both columns are empty
            if (item.isEmpty() && price.isEmpty()) {
                continue;
            }

            GoogleSheetData dto = new GoogleSheetData();
            dto.setItem(item);
            dto.setPrice(price);
            resultList.add(dto);
        }

        // 3. Update Row 1 with "Data inserted" message
        if (!resultList.isEmpty()) {
            updateHeaderAfterInsert(sheetName);
        }

        return resultList;
    }

    @Override
    public List<GoogleSheetUsers> importGoogleSheetUsers() {
        String sheetName = Utils.Sheets.paper4;
        String range = sheetName + "!C4:F"; // C, D, E, F ustunlari 4-qatordan boshlab

        log.info("Fetching users from sheet range: {}", range);
        List<List<Object>> values = readRange(range);

        if (values == null || values.isEmpty()) {
            log.warn("User sheet is empty or could not be read.");
            return Collections.emptyList();
        }

        List<GoogleSheetUsers> resultList = new ArrayList<>();

        for (List<Object> row : values) {
            if (row.isEmpty()) continue;

            GoogleSheetUsers user = new GoogleSheetUsers();

            // C ustuni -> name
            user.setName(row.size() > 0 && row.get(0) != null ? row.get(0).toString().trim() : "");

            // D ustuni -> type
            user.setType(row.size() > 1 && row.get(1) != null ? row.get(1).toString().trim() : "");

            // E ustuni -> contact
            user.setContact(row.size() > 2 && row.get(2) != null ? row.get(2).toString().trim() : "");

            // F ustuni -> comment
            user.setComment(row.size() > 3 && row.get(3) != null ? row.get(3).toString().trim() : "");

            // Kamida ismi yoki kontakti bor qatorlarni qo'shamiz
            if (!user.getName().isEmpty() || !user.getContact().isEmpty()) {
                resultList.add(user);
            }
        }

        log.info("Successfully loaded {} users from Google Sheets", resultList.size());
        return resultList;
    }

    /**
     * Checks if Row 1, Cell A1 starts with "Data updated date"
     */
    private boolean isValidHeader(String headerValue) {
        return headerValue != null && headerValue.startsWith(UPDATED_HEADER_PREFIX);
    }

    /**
     * Updates Row 1, Cell A1 with the current timestamp
     */
    private void updateHeaderAfterInsert(String sheetName) {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT));
        String updateMessage = "Data inserted date " + now;

        String cellAddress = sheetName + "!A1";
        try {
            updateCell(cellAddress, updateMessage);
            log.info("Header updated successfully: {}", updateMessage);
        } catch (Exception e) {
            log.error("Failed to update header cell at {}: ", cellAddress, e);
        }
    }

}