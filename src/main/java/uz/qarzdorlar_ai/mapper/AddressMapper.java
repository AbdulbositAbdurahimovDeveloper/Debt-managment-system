package uz.qarzdorlar_ai.mapper;

import uz.qarzdorlar_ai.model.Address;
import uz.qarzdorlar_ai.payload.AddressDTO;

public interface AddressMapper {
    AddressDTO toDTO(Address address);
}
