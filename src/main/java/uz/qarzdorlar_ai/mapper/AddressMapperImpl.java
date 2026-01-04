package uz.qarzdorlar_ai.mapper;

import org.springframework.stereotype.Component;
import uz.qarzdorlar_ai.model.Address;
import uz.qarzdorlar_ai.payload.AddressDTO;

@Component
public class AddressMapperImpl implements AddressMapper {

    @Override
    public AddressDTO toDTO(Address address) {
        if (address == null)
            return null;

        AddressDTO addressDTO = new AddressDTO();
        addressDTO.setId(addressDTO.getId());
        addressDTO.setRegion(address.getRegion());
        addressDTO.setDistrict(addressDTO.getDistrict());
        addressDTO.setStreetAndHouse(address.getStreetAndHouse());
        addressDTO.setLatitude(address.getLatitude());
        addressDTO.setLongitude(address.getLongitude());
        addressDTO.setLandmark(address.getLandmark());
        addressDTO.setCreatedAt(address.getCreatedAt());

        return addressDTO;
    }
}
