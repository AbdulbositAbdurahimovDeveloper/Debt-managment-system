package uz.qarzdorlar_ai.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uz.qarzdorlar_ai.model.User;
import uz.qarzdorlar_ai.payload.AddressCreateDTO;
import uz.qarzdorlar_ai.payload.AddressDTO;
import uz.qarzdorlar_ai.payload.AddressUpdateDTO;
import uz.qarzdorlar_ai.payload.PageDTO;
import uz.qarzdorlar_ai.payload.response.ResponseDTO;
import uz.qarzdorlar_ai.service.AddressService;

@RestController
@RequestMapping("/api/v1/address")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @PostMapping
    public ResponseEntity<ResponseDTO<AddressDTO>> createAddress(@RequestBody @Valid AddressCreateDTO addressCreateDTO,
                                                                 @AuthenticationPrincipal User user) {

        AddressDTO addressDTO = addressService.createAddress(addressCreateDTO, user);

        return ResponseEntity.ok(ResponseDTO.success(addressDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseDTO<AddressDTO>> getByIdAddress(@PathVariable Long id,
                                                                  @AuthenticationPrincipal User user) {

        AddressDTO addressDTO = addressService.getByIdAddress(id, user);

        return ResponseEntity.ok(ResponseDTO.success(addressDTO));
    }

    @GetMapping("/all")
    public ResponseEntity<ResponseDTO<PageDTO<AddressDTO>>> getAllAddress(@RequestParam(defaultValue = "0") Integer page,
                                                                          @RequestParam(defaultValue = "10") Integer size,
                                                                          @AuthenticationPrincipal User user) {

        PageDTO<AddressDTO> addressDTO = addressService.getAllAddress(page, size, user);

        return ResponseEntity.ok(ResponseDTO.success(addressDTO));

    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseDTO<AddressDTO>> getAllAddress(@PathVariable Long id,
                                                                 @RequestBody @Valid AddressUpdateDTO addressUpdateDTO,
                                                                 @AuthenticationPrincipal User user) {

        AddressDTO addressDTO = addressService.updateAddress(id,addressUpdateDTO, user);

        return ResponseEntity.ok(ResponseDTO.success(addressDTO));

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDTO<String>> deleteAddress(Long id, @AuthenticationPrincipal User user) {

        addressService.deleteAddress(id, user);

        return ResponseEntity.ok(ResponseDTO.success("Address deleted successfully. Deleted address with id : " + id));
    }

}
