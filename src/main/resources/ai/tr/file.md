Senior Java Developer sifatida, tizimingizdagi 6 xil asosiy holat uchun **TransactionCreateDTO** modeliga mos keladigan aniq JSON namunalarini tayyorladim.

Eslatma:
*   **Musbat (+) natija:** Biz qarzdormiz (Mijozning haqi ko'paydi).
*   **Manfiy (-) natija:** Mijoz qarzdor (Mijozning bizdan qarzi ko'paydi).
*   **FeeAmount:** Har doim `transactionCurrency`da yuboriladi.

---

### 1. SALE (Sotuv)
**Vaziyat:** Mijozga so'mda (UZS) tovar sotildi, mijozning balansi dollarda (USD).
*   **Balans ta'siri:** Manfiy (-). Mijoz bizdan qarzdor bo'ladi.
```json
{
  "clientId": 1,
  "type": "SALE",
  "transactionCurrency": "UZS",
  "marketRate": 12800.0,
  "clientRate": 1.0, 
  "feeAmount": 20000.0, 
  "items": [
    {
      "productId": 101,
      "quantity": 2,
      "unitPrice": 500000.0
    }
  ],
  "description": "Mijozga 1 mln so'mlik tovar sotildi, 20 ming dostavka qo'shildi"
}
```

### 2. PURCHASE (Sotib olish)
**Vaziyat:** Taminotchidan dollarda (USD) tovar oldik, taminotchi balansi dollarda (USD).
*   **Balans ta'siri:** Musbat (+). Biz taminotchidan qarzdor bo'lamiz.
```json
{
  "clientId": 2,
  "type": "PURCHASE",
  "transactionCurrency": "USD",
  "marketRate": 1.0,
  "clientRate": 1.0,
  "items": [
    {
      "productId": 202,
      "quantity": 100,
      "unitPrice": 15.0
    }
  ],
  "description": "Taminotchidan 1500$ lik yuk keldi"
}
```

### 3. RETURN (Mijozdan vozvrat)
**Vaziyat:** Mijoz olingan tovarni qaytardi (UZS), balansi Dirhamda (AED).
*   **Balans ta'siri:** Musbat (+). Mijozning qarzi kamayadi (biz unga qarzdor bo'lamiz).
```json
{
  "clientId": 3,
  "type": "RETURN",
  "transactionCurrency": "UZS",
  "marketRate": 12800.0,
  "clientRate": 3.67,
  "feeAmount": 10000.0, 
  "items": [
    {
      "productId": 101,
      "quantity": 1,
      "unitPrice": 500000.0
    }
  ],
  "description": "Mijoz 1ta tovarni qaytardi, 10 ming so'm xizmat haqi ushlab qolindi"
}
```

### 4. CASH_IN (Kirim - Pul olish)
**Vaziyat:** Mijoz qarzini to'lash uchun 10 mln so'm berdi. Mijoz balansi dollarda.
*   **Balans ta'siri:** Musbat (+). Biz pul oldik, qarzimiz ko'paydi (mijozning qarzi yopildi).
```json
{
  "clientId": 1,
  "type": "CASH_IN",
  "transactionCurrency": "UZS",
  "amount": 10000000.0,
  "marketRate": 12800.0,
  "clientRate": 1.0,
  "description": "Mijozdan 10 mln so'm kassa orqali qabul qilindi"
}
```

### 5. CASH_OUT (Chiqim - Pul berish)
**Vaziyat:** Taminotchiga 5000$ berib yubordik. Kuryer 50$ xizmat haqi oldi.
*   **Balans ta'siri:** Manfiy (-). Biz pul berdik, taminotchining bizdagi haqi kamaydi.
```json
{
  "clientId": 2,
  "type": "CASH_OUT",
  "transactionCurrency": "USD",
  "amount": 5050.0,
  "marketRate": 1.0,
  "clientRate": 1.0,
  "feeAmount": 50.0,
  "description": "Taminotchiga kuryer orqali 5000$ berildi, kuryer haqi 50$"
}
```

### 6. TRANSFER (P2P - Kuryerdan Taminotchiga)
**Vaziyat:** Kuryer (Client 5) 10,050$ olib ketdi. Uni Taminotchiga (Client 8) 10,000$ qilib beradi.
*   **Sender (Kuryer):** Balansi manfiyga (-) qarab ketadi (bizdan pulni oldi).
*   **Receiver (Taminotchi):** Balansi musbatga (+) qarab yuradi (unga pul yetib bordi).
```json
{
  "clientId": 5,
  "receiverClientId": 8,
  "type": "TRANSFER",
  "transactionCurrency": "USD",
  "amount": 10050.0,
  "marketRate": 1.0,
  "clientRate": 1.0, 
  "receiverRate": 1.0,
  "feeAmount": 50.0,
  "description": "Kuryerdan taminotchiga o'tkazma"
}
```

### Senior Maslahati:
*   **TRANSFER** holatida `amount`ga kuryer bizdan olib ketgan **jami** summani yozasiz.
*   **FEE** har doim kuryerning haqi hisoblanadi.
*   Agar **SALE** yoki **PURCHASE**da `items` ichidagi `unitPrice` yuborilmasa, backend mahsulotning bazadagi standart narxini avtomatik ishlatadi.

Tizimingiz endi ushbu 6 xil JSON bilan to'liq va sodda ishlaydi!