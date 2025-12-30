package uz.qarzdorlar_ai.scheduling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uz.qarzdorlar_ai.healthInfoBot.HealthBot;
import uz.qarzdorlar_ai.mapper.SendMsg;
import uz.qarzdorlar_ai.payload.ProductInsertEvent;
import uz.qarzdorlar_ai.payload.ProductParseDTO;
import uz.qarzdorlar_ai.payload.sheet.GoogleSheetData;
import uz.qarzdorlar_ai.repository.ProductRepository;
import uz.qarzdorlar_ai.service.ai.AiService;
import uz.qarzdorlar_ai.service.sheet.SheetService;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleSheetDataUpdates {

    @Value("${telegram.health.develop-chat-id}")
    private Long developChatId;

    private final AiService aiService;
    private final SheetService sheetService;
    private final ProductRepository productRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final HealthBot healthBot;
    private final SendMsg sendMsg;

    @Async
//        @Scheduled(cron = "0 0 * * * *") // every hour
    @Scheduled(initialDelay = 0, fixedRate = 60 * 60 * 1000)
    public void updateOrInsertProductDatabase() {
        List<GoogleSheetData> googleSheetData = sheetService.importGoogleSheetData();
        log.info(" 💾 Sheets data size {} ", googleSheetData.size());

        List<GoogleSheetData> toProcess = new ArrayList<>();
        // Bu run davomida dublikatlarni ushlab qolish uchun
        Set<String> alreadyAddedInThisRun = new HashSet<>();

        int itemCount = 1;
        for (GoogleSheetData sheetData : googleSheetData) {
            String item = sheetData.getItem().trim();

            // 1. Agar bu run davomida ro'yxatga qo'shilgan bo'lsa, o'tib ket
            if (alreadyAddedInThisRun.contains(item)) {
                continue;
            }

            if (productRepository.existsByRawData(item)) {
                continue;
            }

            // 2. Bazada borligini tekshirish
            // MUHIM: findByRawData(item.trim()) qilib tekshiring agar AI space qo'shayotgan bo'lsa
            boolean exists = productRepository.existsByRawData(item);

            if (!exists) {
                toProcess.add(sheetData);
                alreadyAddedInThisRun.add(item); // Ro'yxatga qo'shilganini belgilab qo'yamiz
                log.info(" ➕ {} Item added to list for AI: {}", itemCount++, item);
            }
        }

        if (toProcess.isEmpty()) {
            log.info("No new items to process.");
            return;
        }

        infoMessageSheetData(googleSheetData, toProcess);

        List<List<GoogleSheetData>> partitions = partition(toProcess, 10);
        int s = 1;
        for (List<GoogleSheetData> batch : partitions) {
//            List<ProductParseDTO> productParseDTOS = aiService.productParseAI(batch);
            log.info("Batch processed: {}", s++);

//            if (!productParseDTOS.isEmpty()) {
//                applicationEventPublisher.publishEvent(new ProductInsertEvent(productParseDTOS));
//            }
        }
    }


    @Async
    //    @Scheduled(cron = "0 0 * * * *") // every hour
    @Scheduled(initialDelay = 0, fixedRate = 60 * 60 * 1000)
    public void updateOrInsertClientDatabase(){




    }


    private void infoMessageSheetData(List<GoogleSheetData> googleSheetData, List<GoogleSheetData> toProcess) {
        // Metrics calculation
        int totalFetched = googleSheetData.size();
        int deltaNew = toProcess.size();
        int existingInDb = totalFetched - deltaNew;
        int batchCount = (int) Math.ceil((double) deltaNew / 10);

        String infoMsg = String.format("""
                🛠 <b>INVENTORY SYNC ENGINE</b>
                
                📊 <b>Data Ingestion Metrics</b>
                ┣ 📥 Total fetched: <code>%d</code>
                ┣ 🛡 Existing in DB: <code>%d</code> <i>(Skipped)</i>
                ┗ 🎯 Targeted Delta: <b><code>%d</code></b>
                
                ⚙️ <b>Execution Strategy</b>
                ┣ 🤖 Pipeline: <i>AI-driven parsing</i>
                ┗ 📦 Workload: <code>%d</code> batches <i>[Size: 10]</i>
                
                🛰 <b>Status:</b> 🟡 <i>Processing Delta...</i>
                """, totalFetched, existingInDb, deltaNew, batchCount);

// Telegram bot orqali yuborish (parseMode: HTML ekanligiga ishonch hosil qiling)
        healthBot.healthExecute(sendMsg.sendMessage(developChatId, infoMsg));
    }

    /**
     * Listni belgilangan o'lchamdagi kichik listlarga bo'lib beradi (Partitioning).
     *
     * @param list     Asosiy ma'lumotlar listi
     * @param pageSize Har bir ichki listning maksimal o'lchami (sizning holatda 10)
     * @param <T>      List turi
     * @return Bo'lingan listlar listi
     */
    public <T> List<List<T>> partition(List<T> list, int pageSize) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }

        List<List<T>> partitions = new ArrayList<>();
        int size = list.size();

        for (int i = 0; i < size; i += pageSize) {
            // Math.min ishlatishdan maqsad - oxirgi bo'lakda 10 tadan kam qolsa xatolik bermasligi
            partitions.add(new ArrayList<>(
                    list.subList(i, Math.min(size, i + pageSize))
            ));
        }

        return partitions;
    }
}
