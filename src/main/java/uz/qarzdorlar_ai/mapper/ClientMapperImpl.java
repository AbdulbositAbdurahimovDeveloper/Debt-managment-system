package uz.qarzdorlar_ai.mapper;

import org.springframework.stereotype.Component;
import uz.qarzdorlar_ai.model.Client;
import uz.qarzdorlar_ai.payload.ClientDTO;

@Component
public class ClientMapperImpl implements ClientMapper {

    @Override
    public ClientDTO toDTO(Client client) {
        ClientDTO clientDTO = new ClientDTO();

        clientDTO.setId(client.getId());
        clientDTO.setUserId(
                client.getUser() != null
                        ? client.getUser().getId()
                        : null
        );
        clientDTO.setAddress(client.getAddress());
        clientDTO.setComment(client.getComment());
        clientDTO.setType(client.getType());
        clientDTO.setFullName(client.getFullName());
        clientDTO.setPhoneNumber(client.getPhoneNumber());
//        clientDTO.setBalanceCurrencyId(
//                client.getBalanceCurrency() != null
//                        ? client.getBalanceCurrency().getId()
//                        : null
//        );
        clientDTO.setTelegramUserChatId(
                client.getTelegramUser() != null
                        ? client.getTelegramUser().getChatId()
                        : null
        );
        clientDTO.setCurrentBalance(client.getCurrentBalance());
        clientDTO.setDeleted(client.isDeleted());
        clientDTO.setCreatedAt(client.getCreatedAt());
        clientDTO.setUpdatedAt(client.getUpdatedAt());

        return clientDTO;
    }
}
