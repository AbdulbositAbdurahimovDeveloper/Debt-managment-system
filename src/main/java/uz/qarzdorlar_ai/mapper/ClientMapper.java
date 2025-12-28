package uz.qarzdorlar_ai.mapper;

import uz.qarzdorlar_ai.model.Client;
import uz.qarzdorlar_ai.payload.ClientDTO;

public interface ClientMapper {

    ClientDTO toDTO(Client client);

}
