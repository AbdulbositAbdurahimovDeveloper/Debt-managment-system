package uz.qarzdorlar_ai.service.transactions;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.qarzdorlar_ai.exception.BadRequestException;
import uz.qarzdorlar_ai.exception.EntityNotFoundException;
import uz.qarzdorlar_ai.model.*;
import uz.qarzdorlar_ai.repository.ClientRepository;
import uz.qarzdorlar_ai.repository.ExchangeRateRepository;

import java.math.BigDecimal;

/**
 * Transaction helper metodlar uchun service
 * Exchange rate, validation, balance update kabi yordamchi metodlar
 */
@Service
@RequiredArgsConstructor
public class TransactionHelperService {

    private final ExchangeRateRepository exchangeRateRepository;
    private final ClientRepository clientRepository;

    /**
     * Exchange rate olish uchun helper metod
     * Priority: DTO -> DB -> Xatolik
     *
     * @param dtoRate DTO dan kelgan rate (null bo'lishi mumkin)
     * @param currency Currency entity
     * @return Exchange rate
     */
    @Transactional(readOnly = true)
    public BigDecimal getExchangeRate(BigDecimal dtoRate, Currency currency) {
        BigDecimal rate;

        // Agar DTO dan rate kelgan bo'lsa, uni ishlatamiz
        if (dtoRate != null && dtoRate.compareTo(BigDecimal.ZERO) > 0) {
            rate = dtoRate;
        } else if ("USD".equals(currency.getCode())) {
            // Agar USD bo'lsa, 1.0 qaytaramiz
            rate = BigDecimal.ONE;
        } else {
            // DB dan eng oxirgi rate ni olish
            ExchangeRate exchangeRate = exchangeRateRepository
                    .findFirstByCurrencyOrderByCreatedAtDesc(currency)
                    .orElseThrow(() -> new BadRequestException("Please provide exchange rate for currency: " + currency.getCode()));
            rate = exchangeRate.getRate();
        }

        // Sanity check - kurs mantiqiy chegarada bo'lishi kerak
        validateExchangeRate(rate, currency);

        return rate;
    }

    /**
     * Client exchange rate olish uchun helper metod
     * Priority: DTO -> DB -> Xatolik
     *
     * @param dtoRate DTO dan kelgan client exchange rate
     * @param clientCurrency Client ning balance currency
     * @return Client exchange rate
     */
    @Transactional(readOnly = true)
    public BigDecimal getClientExchangeRate(BigDecimal dtoRate, Currency clientCurrency) {
        BigDecimal rate;

        // Agar DTO dan rate kelgan bo'lsa, uni ishlatamiz
        if (dtoRate != null && dtoRate.compareTo(BigDecimal.ZERO) > 0) {
            rate = dtoRate;
        } else if ("USD".equals(clientCurrency.getCode())) {
            // Agar USD bo'lsa, 1.0 qaytaramiz
            rate = BigDecimal.ONE;
        } else {
            // DB dan eng oxirgi rate ni olish
            ExchangeRate exchangeRate = exchangeRateRepository
                    .findFirstByCurrencyOrderByCreatedAtDesc(clientCurrency)
                    .orElseThrow(() -> new BadRequestException("Please provide client exchange rate for currency: " + clientCurrency.getCode()));
            rate = exchangeRate.getRate();
        }

        // Sanity check - kurs mantiqiy chegarada bo'lishi kerak
        validateExchangeRate(rate, clientCurrency);

        return rate;
    }

    /**
     * Exchange rate validation - sanity check
     * Kurslar mantiqiy chegarada bo'lishi kerak (typo va xatoliklardan himoya qilish)
     */
    public void validateExchangeRate(BigDecimal rate, Currency currency) {
        String code = currency.getCode();

        // USD uchun 1.0 bo'lishi kerak
        if ("USD".equals(code)) {
            if (rate.compareTo(BigDecimal.ONE) != 0) {
                throw new BadRequestException("Exchange rate for USD must be 1.0, but received: " + rate);
            }
            return;
        }

        // UZS uchun kurs juda katta bo'lishi kerak (odatda 10000-15000 orasida)
        if ("UZS".equals(code)) {
            if (rate.compareTo(BigDecimal.valueOf(1000)) < 0 || rate.compareTo(BigDecimal.valueOf(50000)) > 0) {
                throw new BadRequestException(
                        String.format("Exchange rate for UZS seems incorrect. Expected range: 1000-50000, but received: %s. " +
                                "Please verify the rate to prevent data corruption.", rate)
                );
            }
            return;
        }

        // AED uchun kurs kichik bo'lishi kerak (odatda 3.5-4.0 orasida)
        if ("AED".equals(code)) {
            if (rate.compareTo(BigDecimal.valueOf(1.0)) < 0 || rate.compareTo(BigDecimal.valueOf(10.0)) > 0) {
                throw new BadRequestException(
                        String.format("Exchange rate for AED seems incorrect. Expected range: 1.0-10.0, but received: %s. " +
                                "Please verify the rate to prevent data corruption.", rate)
                );
            }
            return;
        }

        // Boshqa valyutalar uchun umumiy tekshiruv
        // Kurs 0 dan katta va 100000 dan kichik bo'lishi kerak
        if (rate.compareTo(BigDecimal.ZERO) <= 0 || rate.compareTo(BigDecimal.valueOf(100000)) > 0) {
            throw new BadRequestException(
                    String.format("Exchange rate for %s seems incorrect. Received: %s. " +
                            "Please verify the rate to prevent data corruption.", code, rate)
            );
        }
    }

    /**
     * Client balance yangilash
     * Har bir transaction type uchun balansga ta'sir qiladi
     *
     * MUHIM: Race condition oldini olish uchun SQL darajasida atomic operation ishlatiladi
     * Bu bir vaqtning o'zida bir nechta tranzaksiya kelganda balanslarni to'g'ri yangilashni ta'minlaydi
     *
     * Mantiq: currentBalance manfiy bo'lsa - mijoz bizdan qarz olgan (qarzdor)
     *         currentBalance musbat bo'lsa - biz mijozdan qarz olganmiz (qarzimiz bor)
     */
    @Transactional
    public void updateClientBalance(Transaction transaction) {
        Client client = transaction.getClient();
        BigDecimal balanceAmount = transaction.getBalanceAmount();

        // Transaction type ga qarab balansni yangilash
        // SQL darajasida atomic operation - race condition oldini oladi
        switch (transaction.getType()) {
            case SALE, RETURN_PAYMENT, PURCHASE -> {
                // Qarz ko'payadi: currentBalance = currentBalance - balanceAmount
                // SALE: mijozdan tovar sotiladi, mijozning qarzi ko'payadi (balans kamayadi)
                // RETURN_PAYMENT: to'lov qaytariladi, qarz ko'payadi
                // PURCHASE: supplierdan tovar sotib olinadi, bizning qarzimiz ko'payadi (supplier balansi oshadi)
                int updated = clientRepository.updateBalanceAtomic(client.getId(), balanceAmount.negate());
                if (updated == 0) {
                    throw new BadRequestException("Failed to update client balance. Client may have been deleted or modified.");
                }
            }
            case PAYMENT, RETURN -> {
                // Qarz kamayadi: currentBalance = currentBalance + balanceAmount
                // PAYMENT: mijoz to'lov qiladi, qarzi kamayadi (balans oshadi)
                // RETURN: tovar qaytariladi, qarz kamayadi
                int updated = clientRepository.updateBalanceAtomic(client.getId(), balanceAmount);
                if (updated == 0) {
                    throw new BadRequestException("Failed to update client balance. Client may have been deleted or modified.");
                }
            }
            case TRANSFER -> {
                // Client1 dan pul ayiriladi (qarzi ko'payadi yoki qarzimiz kamayadi)
                int updated1 = clientRepository.updateBalanceAtomic(client.getId(), balanceAmount.negate());
                if (updated1 == 0) {
                    throw new BadRequestException("Failed to update sender client balance. Client may have been deleted or modified.");
                }

                // Client2 ga pul qo'shiladi (qarzi kamayadi yoki qarzimiz ko'payadi)
                Client receiverClient = transaction.getReceiverClient();
                if (receiverClient != null) {
                    // Receiver uchun balance amount hisoblash (receiver valyutasida)
                    BigDecimal receiverBalanceAmount = transaction.getUsdAmount()
                            .multiply(transaction.getReceiverExchangeRate());

                    // Receiver ga pul qo'shiladi (qarzi kamayadi)
                    int updated2 = clientRepository.updateBalanceAtomic(receiverClient.getId(), receiverBalanceAmount);
                    if (updated2 == 0) {
                        throw new BadRequestException("Failed to update receiver client balance. Client may have been deleted or modified.");
                    }
                }
            }
        }
    }

    /**
     * Eski transaction ning balansga ta'sirini qaytarish (revert)
     * Update yoki Delete qilishdan oldin eski balansni qaytarish uchun
     */
    @Transactional
    public void revertClientBalance(Transaction transaction) {
        Client client = transaction.getClient();
        BigDecimal balanceAmount = transaction.getBalanceAmount();

        // Transaction type ga qarab balansni teskari qilish
        switch (transaction.getType()) {
            case SALE, RETURN_PAYMENT, PURCHASE -> {
                // Eski qarzni qaytarish: currentBalance = currentBalance + balanceAmount
                int updated = clientRepository.updateBalanceAtomic(client.getId(), balanceAmount);
                if (updated == 0) {
                    throw new BadRequestException("Failed to revert client balance. Client may have been deleted or modified.");
                }
            }
            case PAYMENT, RETURN -> {
                // Eski to'lovni qaytarish: currentBalance = currentBalance - balanceAmount
                int updated = clientRepository.updateBalanceAtomic(client.getId(), balanceAmount.negate());
                if (updated == 0) {
                    throw new BadRequestException("Failed to revert client balance. Client may have been deleted or modified.");
                }
            }
            case TRANSFER -> {
                // Client1 ga qaytarish
                int updated1 = clientRepository.updateBalanceAtomic(client.getId(), balanceAmount);
                if (updated1 == 0) {
                    throw new BadRequestException("Failed to revert sender client balance. Client may have been deleted or modified.");
                }

                // Client2 dan ayirish
                Client receiverClient = transaction.getReceiverClient();
                if (receiverClient != null) {
                    // Receiver exchange rate ni olish
                    BigDecimal receiverExchangeRate = transaction.getReceiverExchangeRate();
                    if (receiverExchangeRate == null) {
                        // Agar receiverExchangeRate null bo'lsa, receiver client ning balance currency dan olamiz
                        Currency receiverCurrency = receiverClient.getBalanceCurrency();
                        if ("USD".equals(receiverCurrency.getCode())) {
                            receiverExchangeRate = BigDecimal.ONE;
                        } else {
                            ExchangeRate rate = exchangeRateRepository
                                    .findFirstByCurrencyOrderByCreatedAtDesc(receiverCurrency)
                                    .orElseThrow(() -> new BadRequestException("Receiver exchange rate not found for currency: " + receiverCurrency.getCode()));
                            receiverExchangeRate = rate.getRate();
                        }
                    }

                    BigDecimal receiverBalanceAmount = transaction.getUsdAmount()
                            .multiply(receiverExchangeRate);

                    int updated2 = clientRepository.updateBalanceAtomic(receiverClient.getId(), receiverBalanceAmount.negate());
                    if (updated2 == 0) {
                        throw new BadRequestException("Failed to revert receiver client balance. Client may have been deleted or modified.");
                    }
                }
            }
        }
    }
}