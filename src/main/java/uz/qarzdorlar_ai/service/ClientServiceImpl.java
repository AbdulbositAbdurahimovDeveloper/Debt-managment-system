package uz.qarzdorlar_ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import uz.qarzdorlar_ai.model.Client;
import uz.qarzdorlar_ai.model.Currency;
import uz.qarzdorlar_ai.payload.PageDTO;
import uz.qarzdorlar_ai.payload.ClientDTO;
import uz.qarzdorlar_ai.mapper.ClientMapper;
import uz.qarzdorlar_ai.payload.ClientUpdateDTO;
import uz.qarzdorlar_ai.payload.ClientCreateDTO;
import uz.qarzdorlar_ai.repository.ClientRepository;
import uz.qarzdorlar_ai.repository.CurrencyRepository;
import uz.qarzdorlar_ai.exception.DataConflictException;
import uz.qarzdorlar_ai.exception.EntityNotFoundException;

@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {

    private final ClientMapper clientMapper;
    private final ClientRepository clientRepository;
    private final CurrencyRepository currencyRepository;

    @Override
    public ClientDTO createClient(ClientCreateDTO clientCreateDTO) {

        Long currencyId = clientCreateDTO.getCurrencyId();
        Currency currency = currencyRepository.findById(currencyId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Currency not found with id : " + currencyId)
                );

        String fullName = clientCreateDTO.getFullName();
        if (clientRepository.existsByFullName(fullName)) {
            throw new DataConflictException("Full name all ready exist with full name : " + fullName);
        }

        Client client = new Client();
        client.setFullName(clientCreateDTO.getFullName());
        client.setPhoneNumber(clientCreateDTO.getPhoneNumber());
        client.setType(clientCreateDTO.getType());
        client.setBalanceCurrency(currency);
        client.setInitialBalance(clientCreateDTO.getInitialBalance());
        client.setAddress(clientCreateDTO.getAddress());
        client.setComment(clientCreateDTO.getComment());

        clientRepository.save(client);

        return clientMapper.toDTO(client);
    }

    @Override
    public ClientDTO getByIdClient(Long id) {

        Client client = clientRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Client not found with id : " + id)
                );

        return clientMapper.toDTO(client);
    }

    @Override
    public PageDTO<ClientDTO> getAllClient(Integer page, Integer size) {

        Sort sort = Sort.by(Client.Fields.fullName);
        PageRequest pageRequest = PageRequest.of(page, size, sort);

        Page<Client> clientPage = clientRepository.findAll(pageRequest);

        return new PageDTO<>(
                clientPage.getContent().stream().map(clientMapper::toDTO).toList(),
                clientPage
        );
    }

    @Override
    public ClientDTO updateClient(Long id, ClientUpdateDTO dto) {

        Client client = clientRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Client not found with id: " + id)
                );

        if (dto.getFullName() != null &&
                !dto.getFullName().equals(client.getFullName())) {

            if (clientRepository.existsByFullName(dto.getFullName())) {
                throw new DataConflictException(
                        "Client already exists with full name: " + dto.getFullName()
                );
            }
            client.setFullName(dto.getFullName());
        }

        if (dto.getPhoneNumber() != null &&
                !dto.getPhoneNumber().equals(client.getPhoneNumber())) {

            if (clientRepository.existsByPhoneNumber(dto.getPhoneNumber())) {
                throw new DataConflictException(
                        "Client already exists with phone number: " + dto.getPhoneNumber()
                );
            }
            client.setPhoneNumber(dto.getPhoneNumber());
        }

        if (dto.getType() != null) {
            client.setType(dto.getType());
        }

        if (dto.getAddress() != null) {
            client.setAddress(dto.getAddress());
        }

        if (dto.getComment() != null) {
            client.setComment(dto.getComment());
        }

        clientRepository.save(client);
        return clientMapper.toDTO(client);
    }


    @Override
    public String deleteClient(Long id) {

        Client client = clientRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Client not found with id: " + id)
                );

        clientRepository.delete(client);

        return "Client deleted successfully with client id : " + id;
    }
}
