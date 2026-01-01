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

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class WorkSheetServiceImpl implements WorkSheetService{

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

}
