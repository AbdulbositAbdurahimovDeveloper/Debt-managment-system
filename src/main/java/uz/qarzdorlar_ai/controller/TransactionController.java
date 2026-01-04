package uz.qarzdorlar_ai.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uz.qarzdorlar_ai.model.User;
import uz.qarzdorlar_ai.payload.PageDTO;
import uz.qarzdorlar_ai.payload.TransactionCreateDTO;
import uz.qarzdorlar_ai.payload.TransactionDTO;
import uz.qarzdorlar_ai.payload.TransactionUpdateDTO;
import uz.qarzdorlar_ai.payload.response.ResponseDTO;
import uz.qarzdorlar_ai.service.transactions.embedded.TransactionService;

//@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api/v1/transaction")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<ResponseDTO<?>> createTransaction(@Valid @RequestBody TransactionCreateDTO dto,
                                                            @AuthenticationPrincipal User staffUser) {

        TransactionDTO transactionDTO = transactionService.createTransaction(dto, staffUser);

        return ResponseEntity.ok(ResponseDTO.success(transactionDTO));

    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseDTO<TransactionDTO>> getByIdTransaction(@PathVariable Long id) {

        TransactionDTO transactionDTO = transactionService.getByIdTransection(id);

        return ResponseEntity.ok(ResponseDTO.success(transactionDTO));

    }

    @GetMapping()
    public ResponseEntity<ResponseDTO<PageDTO<TransactionDTO>>> getByAllTransaction(@RequestParam(defaultValue = "0") Integer page,
                                                                                    @RequestParam(defaultValue = "10") Integer size) {

        PageDTO<TransactionDTO> transactionDTO = transactionService.getByAllTransection(page, size);

        return ResponseEntity.ok(ResponseDTO.success(transactionDTO));

    }


    @PutMapping("/{id}")
    public ResponseEntity<ResponseDTO<TransactionDTO>> updateTransaction(@PathVariable Long id,
                                                                         @Valid @RequestBody TransactionUpdateDTO dto,
                                                                         @AuthenticationPrincipal User staffUser) {

        TransactionDTO transactionDTO = transactionService.updateTransaction(id, dto, staffUser);

        return ResponseEntity.ok(ResponseDTO.success(transactionDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDTO<String>> deleteTransaction(@PathVariable Long id,
                                                                 @AuthenticationPrincipal User staffUser) {

        transactionService.deleteTransaction(id, staffUser);

        return ResponseEntity.ok(ResponseDTO.success("Transaction deleted successfully"));
    }

    @GetMapping("/by-client/{clientId}")
    public ResponseEntity<ResponseDTO<PageDTO<TransactionDTO>>> getAllTransactionByClientId(@PathVariable Long clientId,
                                                                                            @RequestParam(defaultValue = "0") Integer page,
                                                                                            @RequestParam(defaultValue = "10") Integer size) {

        PageDTO<TransactionDTO> transactionDTO = transactionService.getAllTransactionByClientId(clientId, page, size);

        return ResponseEntity.ok(ResponseDTO.success(transactionDTO));
    }

}
