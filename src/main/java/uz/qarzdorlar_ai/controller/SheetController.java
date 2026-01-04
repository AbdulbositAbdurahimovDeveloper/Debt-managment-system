package uz.qarzdorlar_ai.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uz.qarzdorlar_ai.model.User;
import uz.qarzdorlar_ai.payload.TransactionCreateDTO;
import uz.qarzdorlar_ai.payload.TransactionDTO;
import uz.qarzdorlar_ai.payload.sheet.WorkJournalDTO;
import uz.qarzdorlar_ai.service.ai.AiService;
import uz.qarzdorlar_ai.service.sheet.TransactionImportService;
import uz.qarzdorlar_ai.service.sheet.WorkSheetService;
import uz.qarzdorlar_ai.service.transactions.embedded.TransactionService;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/open/work/sheet")
@RequiredArgsConstructor
public class SheetController {

    private final WorkSheetService workSheetService;
    private final TransactionService transactionService;
    private final TransactionImportService transactionImportService;
//    private final AiService aiService;

    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importTransactions(@RequestBody List<TransactionCreateDTO> aiBatch,
                                                                  @AuthenticationPrincipal User user) {
        long startTime = System.currentTimeMillis();
        log.info("================================================================");
        log.info("🚀 IMPORT JARAYONI BOSHLANDI | Jami: {} ta tranzaksiya", aiBatch.size());
        log.info("================================================================");

        int successCount = 0;
        int errorCount = 0;
        List<String> errorMessages = new ArrayList<>();

        for (int i = 0; i < aiBatch.size(); i++) {
            TransactionCreateDTO dto = aiBatch.get(i);
            String rowInfo = String.format("Qator %-3d | Client: %-3d | %-15s | %10.2f %s",
                    (i + 1), dto.getClientId(), dto.getType(), dto.getAmount(), dto.getTransactionCurrency());

            try {
                // Tranzaksiyani saqlash
                TransactionDTO saved = transactionService.createTransaction(dto, user);

                successCount++;
                log.info("✅ SUCCESS | {} | DB_ID: {}", rowInfo, saved.getId());

            } catch (Exception e) {
                errorCount++;
                String errorLog = String.format("❌ ERROR   | %s | Xato: %s", rowInfo, e.getMessage());
                log.error(errorLog);
                errorMessages.add(errorLog);
            }
        }

        long duration = System.currentTimeMillis() - startTime;

        // YAKUNIY HISOBOT LOGDA
        log.info("================================================================");
        log.info("🏁 IMPORT YAKUNLANDI!");
        log.info("⏱ Sarflangan vaqt: {} ms", duration);
        log.info("✅ Muvaffaqiyatli: {}", successCount);
        log.info("⚠️ Xatoliklar:     {}", errorCount);
        log.info("================================================================");

        // Response qaytarish
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "COMPLETED");
        response.put("total", aiBatch.size());
        response.put("success", successCount);
        response.put("failed", errorCount);
        response.put("duration_ms", duration);
        if (!errorMessages.isEmpty()) {
            response.put("errors", errorMessages);
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{type}/{suplier}")
    public Object importSheet(@PathVariable String type, @PathVariable String suplier) {

        return workSheetService.findByTypeAndSupplier(type, suplier);

    }

    @GetMapping("/{id}")
    public Object getString(@PathVariable Long id) {
        List<TransactionCreateDTO> transactionCreateDTOS = transactionImportService.importTrDTO();

        List<TransactionCreateDTO> tr = new ArrayList<>();

        for (TransactionCreateDTO transactionCreateDTO : transactionCreateDTOS) {
            if (Objects.equals(transactionCreateDTO.getClientId(), id))
                tr.add(transactionCreateDTO);
        }

        return tr;
    }

    @GetMapping("/tr/import")
    public Map<String, Object> importTx(@AuthenticationPrincipal User user) {
        // 1. Google Sheets'dan ma'lumotlarni o'qib olish
        List<TransactionCreateDTO> transactionCreateDTOS = transactionImportService.importTrDTO();


        int total = transactionCreateDTOS.size();
        int successCount = 0;
        int failCount = 0;

        // Xatolarni saqlash uchun ro'yxat
        List<Map<String, Object>> errorDetails = new ArrayList<>();

        // 2. Tsikl orqali bittalab saqlash
        for (int i = 0; i < transactionCreateDTOS.size(); i++) {
            TransactionCreateDTO dto = transactionCreateDTOS.get(i);

            // Google Sheetda 2-qatordan o'qishni boshlaganmiz (!C2),
            // shuning uchun i + 2 qator raqamini beradi
            int rowNumber = i + 2;

            try {
                transactionService.createTransaction(dto, user);
                successCount++;
            } catch (Exception e) {
                failCount++;

                // Xato haqida ma'lumotni Map ichiga solish
                Map<String, Object> errorMap = new LinkedHashMap<>();
                errorMap.put("row", rowNumber);
                errorMap.put("clientId", dto.getClientId());
                errorMap.put("type", dto.getType());
                errorMap.put("reason", e.getMessage());

                errorDetails.add(errorMap);
            }
        }

        // 3. Yakuniy natijani Map formatida yig'ish
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "Completed");
        response.put("total_records", total);
        response.put("success_count", successCount);
        response.put("fail_count", failCount);
        response.put("errors", errorDetails);

        return response;
    }
}
