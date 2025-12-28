package uz.qarzdorlar_ai.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.qarzdorlar_ai.payload.ClientCreateDTO;
import uz.qarzdorlar_ai.payload.ClientDTO;
import uz.qarzdorlar_ai.payload.ClientUpdateDTO;
import uz.qarzdorlar_ai.payload.PageDTO;
import uz.qarzdorlar_ai.payload.response.ResponseDTO;
import uz.qarzdorlar_ai.service.ClientService;


@RestController
@RequestMapping("/api/v1/clinet")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    @PostMapping
    public ResponseEntity<ResponseDTO<ClientDTO>> createBrand(@Valid @RequestBody ClientCreateDTO clientCreateDTO) {

        ClientDTO brandDTO = clientService.createClient(clientCreateDTO);

        return ResponseEntity.ok(ResponseDTO.success(brandDTO));

    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseDTO<ClientDTO>> getByIdClient(@PathVariable Long id) {

        ClientDTO brandDTO = clientService.getByIdClient(id);

        return ResponseEntity.ok(ResponseDTO.success(brandDTO));

    }

    @GetMapping("/all")
    public ResponseEntity<ResponseDTO<PageDTO<ClientDTO>>> getAllClient(@RequestParam(defaultValue = "0") Integer page,
                                                                        @RequestParam(defaultValue = "10") Integer size) {

        PageDTO<ClientDTO> brandDTOPageDTO = clientService.getAllClient(page, size);

        return ResponseEntity.ok(ResponseDTO.success(brandDTOPageDTO));

    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseDTO<ClientDTO>> updateClient(@PathVariable Long id,
                                                                   @Valid @RequestBody ClientUpdateDTO clientUpdateDTO) {

        ClientDTO brandDTO = clientService.updateClient(id, clientUpdateDTO);

        return ResponseEntity.ok(ResponseDTO.success(brandDTO));

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDTO<?>> deleteClient(@PathVariable Long id) {

        String deleteClient = clientService.deleteClient(id);

        return ResponseEntity.ok(ResponseDTO.success(deleteClient));

    }


}
