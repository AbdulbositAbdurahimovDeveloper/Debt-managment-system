package uz.qarzdorlar_ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import uz.qarzdorlar_ai.enums.Role;
import uz.qarzdorlar_ai.exception.AccessDeniedException;
import uz.qarzdorlar_ai.exception.EntityNotFoundException;
import uz.qarzdorlar_ai.mapper.AddressMapper;
import uz.qarzdorlar_ai.model.Address;
import uz.qarzdorlar_ai.model.Client;
import uz.qarzdorlar_ai.model.User;
import uz.qarzdorlar_ai.payload.AddressCreateDTO;
import uz.qarzdorlar_ai.payload.AddressDTO;
import uz.qarzdorlar_ai.payload.AddressUpdateDTO;
import uz.qarzdorlar_ai.payload.PageDTO;
import uz.qarzdorlar_ai.repository.AddressRepository;
import uz.qarzdorlar_ai.repository.ClientRepository;
import uz.qarzdorlar_ai.repository.UserRepository;

import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final AddressMapper addressMapper;
    private final UserRepository userRepository;

    private static final EnumSet<Role> ALLOWED_ROLES = EnumSet.of(Role.ADMIN, Role.DEVELOPER, Role.STAFF, Role.STAFF_PLUS);
    private final ClientRepository clientRepository;

    @Override
    public AddressDTO createAddress(AddressCreateDTO dto, User user) {

        User createdByUser = userRepository.findByUsername(user.getUsername())
                .orElseThrow(() ->
                        new EntityNotFoundException("User not found with username : " + user.getUsername())
                );

        Address address = new Address();
        address.setRegion(dto.getRegion());
        address.setDistrict(dto.getDistrict());
        address.setStreetAndHouse(dto.getStreetAndHouse());
        address.setLatitude(dto.getLatitude());
        address.setLongitude(address.getLongitude());
        address.setLandmark(address.getLandmark());

        address.setCreatedBy(createdByUser);

        Address save = addressRepository.save(address);

        return addressMapper.toDTO(save);
    }

    @Override
    public AddressDTO getByIdAddress(Long id, User user) {

        User currentUser = userRepository.findByUsername(user.getUsername())
                .orElseThrow(() ->
                        new EntityNotFoundException("User not found with username : " + user.getUsername())
                );

        Role role = currentUser.getRole();

        Address address;

        if (ALLOWED_ROLES.contains(role)) {
            address = addressRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Address not found with id : " + id));
        } else {
            address = addressRepository.findByIdAndCreatedBy(id, currentUser)
                    .orElseThrow(() -> new EntityNotFoundException("Address not found with id : " + id));
        }

        return addressMapper.toDTO(address);
    }


    @Override
    public PageDTO<AddressDTO> getAllAddress(Integer page, Integer size, User user) {

        Sort sort = Sort.by(Address.Fields.region);
        PageRequest pageRequest = PageRequest.of(page, size, sort);

        User currentUser = userRepository.findByUsername(user.getUsername())
                .orElseThrow(() ->
                        new EntityNotFoundException("User not found with username : " + user.getUsername())
                );

        Page<Address> addressPage;

        if (ALLOWED_ROLES.contains(currentUser.getRole())) {
            addressPage = addressRepository.findAll(pageRequest);
        } else {
            addressPage = addressRepository.findAllByCreatedBy(currentUser, pageRequest);
        }

        List<AddressDTO> dtos = addressPage.stream()
                .map(addressMapper::toDTO)
                .toList();

        return new PageDTO<>(dtos, addressPage);

    }


    @Override
    public AddressDTO updateAddress(Long id, AddressUpdateDTO addressUpdateDTO, User user) {

        User currentUser = userRepository.findByUsername(user.getUsername())
                .orElseThrow(() ->
                        new EntityNotFoundException("User not found with username: " + user.getUsername())
                );

        Address address;

        if (ALLOWED_ROLES.contains(currentUser.getRole())) {
            address = addressRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Address not found with id: " + id));
        } else {
            address = addressRepository.findByIdAndCreatedBy(id, currentUser)
                    .orElseThrow(() -> new EntityNotFoundException("Address not found or you are not allowed to update this address"));
        }

        if (addressUpdateDTO.getRegion() != null) {
            address.setRegion(addressUpdateDTO.getRegion());
        }
        if (addressUpdateDTO.getDistrict() != null) {
            address.setDistrict(addressUpdateDTO.getDistrict());
        }
        if (addressUpdateDTO.getStreetAndHouse() != null) {
            address.setStreetAndHouse(addressUpdateDTO.getStreetAndHouse());
        }
        if (addressUpdateDTO.getLatitude() != null) {
            address.setLatitude(addressUpdateDTO.getLatitude());
        }
        if (addressUpdateDTO.getLongitude() != null) {
            address.setLongitude(addressUpdateDTO.getLongitude());
        }
        if (addressUpdateDTO.getLandmark() != null) {
            address.setLandmark(addressUpdateDTO.getLandmark());
        }

        addressRepository.save(address);

        return addressMapper.toDTO(address);
    }


    @Override
    public void deleteAddress(Long id, User user) {

        User currentUser = userRepository.findByUsername(user.getUsername())
                .orElseThrow(() ->
                        new EntityNotFoundException("User not found with username: " + user.getUsername())
                );

        Address address;

        if (ALLOWED_ROLES.contains(currentUser.getRole())) {
            address = addressRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Address not found with id: " + id));
        } else {
            address = addressRepository.findByIdAndCreatedBy(id, currentUser)
                    .orElseThrow(() -> new AccessDeniedException("You are not allowed to delete this address or it does not exist"));
        }

        Client client = address.getClient();
        client.setAddress(null);
        clientRepository.save(client);
        addressRepository.delete(address);
    }

}
