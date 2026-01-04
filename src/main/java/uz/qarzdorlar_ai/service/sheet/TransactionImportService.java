package uz.qarzdorlar_ai.service.sheet;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uz.qarzdorlar_ai.enums.CurrencyCode;
import uz.qarzdorlar_ai.enums.TransactionType;
import uz.qarzdorlar_ai.payload.TransactionCreateDTO;
import uz.qarzdorlar_ai.payload.TransactionItemCreateDTO;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionImportService {

    private final ObjectMapper objectMapper;
    private final WorkSheetService workSheetService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public List<TransactionCreateDTO> importTrDTO() {
        String sheetName = "transaction";
        String range = String.format("'%s'!C2:N101", sheetName);

        List<List<Object>> lists = workSheetService.readRange(range);

        if (lists == null || lists.isEmpty()) {
            return Collections.emptyList();
        }

        List<TransactionCreateDTO> dtos = new ArrayList<>();
        for (int i = 0; i < lists.size(); i++) {
            try {
                dtos.add(mapRowToDto(lists.get(i)));
            } catch (Exception e) {
                log.error("Xatolik qatorda {}: {}", (i + 2), e.getMessage());
            }
        }
        return dtos;
    }

    private TransactionCreateDTO mapRowToDto(List<Object> row) {
        TransactionCreateDTO dto = new TransactionCreateDTO();

        // 0: clientId (C)
        dto.setClientId(parseLong(getVal(row, 0)));

        // 1: receiverClientId (D)
        dto.setReceiverClientId(parseLong(getVal(row, 1)));

        // 2: type (E)
        String typeStr = getVal(row, 2);
        if (typeStr != null) dto.setType(TransactionType.valueOf(typeStr.toUpperCase()));

        // 3: transactionCurrency (F)
        String currStr = getVal(row, 3);
        if (currStr != null) dto.setTransactionCurrency(CurrencyCode.valueOf(currStr.toUpperCase()));

        // 4: amount (G) - "8 000,00" -> 8000.00
        dto.setAmount(parseBigDecimal(getVal(row, 4)));

        // 5: rateToUsd (H) - "3,665" -> 3.665
        dto.setRateToUsd(parseBigDecimal(getVal(row, 5)));

        // 6: clientRateToUsd (I)
        dto.setClientRateToUsd(parseBigDecimal(getVal(row, 6)));

        // 7: receiverRateToUsd (J)
        dto.setReceiverRateToUsd(parseBigDecimal(getVal(row, 7)));

        // 8: feeAmount (K)
        dto.setFeeAmount(parseBigDecimal(getVal(row, 8)));

        // 9: description (L)
        dto.setDescription(getVal(row, 9));

        // 10: items (M) - JSON parsing
        String itemsJson = getVal(row, 10);
        if (itemsJson != null) {
            try {
                objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
                objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
                dto.setItems(objectMapper.readValue(itemsJson, new TypeReference<>() {}));
            } catch (Exception e) {
                log.warn("JSON xatosi: {}", itemsJson);
            }
        }

        // 11: create_at (N) - "22.10.2025" -> Timestamp
        String dateStr = getVal(row, 11);
        if (dateStr != null) {
            try {
                LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
                dto.setCreatedAt(Timestamp.valueOf(date.atStartOfDay()));
            } catch (Exception e) {
                log.warn("Sana formati xato: {}", dateStr);
            }
        }

        return dto;
    }

    // --- TOZALOVCHI VA PARSE QILUVCHI METODLAR ---

    private String getVal(List<Object> row, int index) {
        if (index >= row.size() || row.get(index) == null) return null;
        String s = row.get(index).toString().trim();
        return s.isEmpty() ? null : s;
    }

    private Long parseLong(String val) {
        if (val == null) return null;
        try {
            // "2.0" yoki "7 " kabi holatlarni tozalash
            String clean = val.split("\\.")[0].replaceAll("[^0-9]", "");
            return Long.parseLong(clean);
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal parseBigDecimal(String val) {
        if (val == null || val.trim().isEmpty()) {
            // null qaytarish o'rniga ZERO qaytaramiz
            return BigDecimal.ZERO;
        }
        try {
            // Bo'shliqlarni olib tashlash (non-breaking space bilan birga)
            String clean = val.replaceAll("[\\s\\u00A0]+", "");
            // Vergulni nuqtaga almashtirish
            clean = clean.replace(",", ".");

            // Agar tozalashdan keyin ham bo'sh bo'lib qolsa
            if (clean.isEmpty() || clean.equals(".")) return BigDecimal.ZERO;

            return new BigDecimal(clean);
        } catch (Exception e) {
            log.warn("BigDecimal parse xatosi (qiymat: {}), 0 olindi", val);
            return BigDecimal.ZERO; // Xato bo'lsa ham 0 qaytaramiz
        }
    }
}