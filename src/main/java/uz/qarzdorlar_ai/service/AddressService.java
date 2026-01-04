package uz.qarzdorlar_ai.service;

import uz.qarzdorlar_ai.model.User;
import uz.qarzdorlar_ai.payload.AddressCreateDTO;
import uz.qarzdorlar_ai.payload.AddressDTO;
import uz.qarzdorlar_ai.payload.AddressUpdateDTO;
import uz.qarzdorlar_ai.payload.PageDTO;

public interface AddressService {

    AddressDTO createAddress(AddressCreateDTO addressCreateDTO, User user);


    AddressDTO getByIdAddress(Long id, User user);

    PageDTO<AddressDTO> getAllAddress(Integer page, Integer size, User user);

    AddressDTO updateAddress(Long id, AddressUpdateDTO addressUpdateDTO, User user);

    void deleteAddress(Long id, User user);
}
