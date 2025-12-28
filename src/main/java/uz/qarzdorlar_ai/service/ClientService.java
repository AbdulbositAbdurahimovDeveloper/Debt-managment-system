package uz.qarzdorlar_ai.service;

import uz.qarzdorlar_ai.payload.ClientCreateDTO;
import uz.qarzdorlar_ai.payload.ClientDTO;
import uz.qarzdorlar_ai.payload.ClientUpdateDTO;
import uz.qarzdorlar_ai.payload.PageDTO;

public interface ClientService {
    ClientDTO createClient(ClientCreateDTO clientCreateDTO);

    ClientDTO getByIdClient(Long id);

    PageDTO<ClientDTO> getAllClient(Integer page, Integer size);

    ClientDTO updateClient(Long id, ClientUpdateDTO clientUpdateDTO);

    String deleteClient(Long id);
}
