package br.com.marazulturismo.marazulbackendadmin.service;

import br.com.marazulturismo.marazulbackendadmin.dto.AddressRequestDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.ClientDetailResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.ClientListResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.ClientRequestDTO;
import br.com.marazulturismo.marazulbackendadmin.exception.ClientNotFoundException;
import br.com.marazulturismo.marazulbackendadmin.exception.ClientValidationException;
import br.com.marazulturismo.marazulbackendadmin.model.Address;
import br.com.marazulturismo.marazulbackendadmin.model.Client;
import br.com.marazulturismo.marazulbackendadmin.repository.ClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class ClientService {

    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Transactional(readOnly = true)
    public List<ClientListResponseDTO> list(String search) {
        List<Client> clients;

        if (search == null || search.isBlank()) {
            clients = clientRepository.findAll();
        } else {
            String digitsOnly = search.replaceAll("\\D", "");
            clients = clientRepository.searchByNameOrDocument(search, digitsOnly);
        }

        return clients.stream()
                .map(ClientListResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClientDetailResponseDTO findById(Long id) {
        return ClientDetailResponseDTO.fromEntity(findEntity(id));
    }

    @Transactional
    public ClientDetailResponseDTO create(ClientRequestDTO dto) {
        String cpf = ClientRequestDTO.normalizeDocument(dto.cpf());
        String cnpj = ClientRequestDTO.normalizeDocument(dto.cnpj());

        validateDocuments(cpf, cnpj, null);

        Address address = buildAddress(dto.address());

        Client client = new Client(dto.name(), cpf, cnpj, address);

        return ClientDetailResponseDTO.fromEntity(clientRepository.save(client));
    }

    private void validateDocuments(String cpf, String cnpj, Long excludeId) {
        boolean hasCpf = cpf != null && !cpf.isBlank();
        boolean hasCnpj = cnpj != null && !cnpj.isBlank();

        if (hasCpf && hasCnpj) {
            throw new ClientValidationException("Informe apenas CPF ou CNPJ, não ambos.");
        }

        if (!hasCpf && !hasCnpj) {
            throw new ClientValidationException("É obrigatório informar CPF ou CNPJ.");
        }

        if (hasCpf) {
            validateCpf(cpf);

            boolean exists = excludeId == null
                    ? clientRepository.existsByCpf(cpf)
                    : clientRepository.existsByCpfAndIdNot(cpf, excludeId);

            if (exists) {
                throw new ClientValidationException("CPF já cadastrado: " + cpf);
            }
        }

        if (hasCnpj) {
            validateCnpj(cnpj);

            boolean exists = excludeId == null
                    ? clientRepository.existsByCnpj(cnpj)
                    : clientRepository.existsByCnpjAndIdNot(cnpj, excludeId);

            if (exists) {
                throw new ClientValidationException("CNPJ já cadastrado: " + cnpj);
            }
        }
    }

    /**
     * Validação algorítmica do CPF (11 dígitos, cálculo dos dígitos verificadores).
     */
    private void validateCpf(String cpf) {
        if (cpf.length() != 11) {
            throw new ClientValidationException("CPF deve conter 11 dígitos.");
        }

        // Rejeita CPFs com todos os dígitos iguais (ex: 111.111.111-11)
        if (cpf.chars().distinct().count() == 1) {
            throw new ClientValidationException("CPF inválido.");
        }

        int[] digits = cpf.chars().map(c -> c - '0').toArray();

        // Primeiro dígito verificador
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += digits[i] * (10 - i);
        }
        int firstCheck = 11 - (sum % 11);
        if (firstCheck >= 10) firstCheck = 0;

        if (digits[9] != firstCheck) {
            throw new ClientValidationException("CPF inválido.");
        }

        // Segundo dígito verificador
        sum = 0;
        for (int i = 0; i < 10; i++) {
            sum += digits[i] * (11 - i);
        }
        int secondCheck = 11 - (sum % 11);
        if (secondCheck >= 10) secondCheck = 0;

        if (digits[10] != secondCheck) {
            throw new ClientValidationException("CPF inválido.");
        }
    }

    /**
     * Validação algorítmica do CNPJ (14 dígitos, cálculo dos dígitos verificadores).
     */
    private void validateCnpj(String cnpj) {
        if (cnpj.length() != 14) {
            throw new ClientValidationException("CNPJ deve conter 14 dígitos.");
        }

        if (cnpj.chars().distinct().count() == 1) {
            throw new ClientValidationException("CNPJ inválido.");
        }

        int[] digits = cnpj.chars().map(c -> c - '0').toArray();

        // Primeiro dígito verificador
        int[] weights1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            sum += digits[i] * weights1[i];
        }
        int firstCheck = sum % 11 < 2 ? 0 : 11 - (sum % 11);

        if (digits[12] != firstCheck) {
            throw new ClientValidationException("CNPJ inválido.");
        }

        // Segundo dígito verificador
        int[] weights2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        sum = 0;
        for (int i = 0; i < 13; i++) {
            sum += digits[i] * weights2[i];
        }
        int secondCheck = sum % 11 < 2 ? 0 : 11 - (sum % 11);

        if (digits[13] != secondCheck) {
            throw new ClientValidationException("CNPJ inválido.");
        }
    }

    private Address buildAddress(AddressRequestDTO dto) {
        if (dto == null) {
            return null;
        }
        return new Address(
                dto.street(),
                dto.number(),
                dto.complement(),
                dto.city(),
                dto.state() != null ? dto.state().toUpperCase(Locale.ROOT) : null
        );
    }

    private Client findEntity(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new ClientNotFoundException(id));
    }
}
