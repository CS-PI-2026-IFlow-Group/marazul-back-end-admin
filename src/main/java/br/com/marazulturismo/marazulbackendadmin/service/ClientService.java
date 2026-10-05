package br.com.marazulturismo.marazulbackendadmin.service;

import br.com.marazulturismo.marazulbackendadmin.dto.AddressRequestDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.ClientRequestDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.ClientResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.exception.ClientNotFoundException;
import br.com.marazulturismo.marazulbackendadmin.exception.ClientValidationException;
import br.com.marazulturismo.marazulbackendadmin.model.Address;
import br.com.marazulturismo.marazulbackendadmin.model.City;
import br.com.marazulturismo.marazulbackendadmin.model.Client;
import br.com.marazulturismo.marazulbackendadmin.repository.AddressRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.CityRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.ClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClientService {

    private final ClientRepository clientRepository;
    private final AddressRepository addressRepository;
    private final CityRepository cityRepository;

    public ClientService(
            ClientRepository clientRepository,
            AddressRepository addressRepository,
            CityRepository cityRepository) {
        this.clientRepository = clientRepository;
        this.addressRepository = addressRepository;
        this.cityRepository = cityRepository;
    }

    @Transactional
    public ClientResponseDTO update(Long id, ClientRequestDTO dto) {
        Client client = findEntity(id);
        String cpf = ClientRequestDTO.normalizeDocument(dto.cpf());
        String cnpj = ClientRequestDTO.normalizeDocument(dto.cnpj());

        validateDocuments(cpf, cnpj, id);

        City city = cityRepository.findById(dto.address().cityId())
                .orElseThrow(() -> new ClientValidationException("Cidade não encontrada."));

        client.update(dto.name().trim(), cpf, cnpj);
        updateAddress(client, dto.address(), city);

        return ClientResponseDTO.fromEntity(clientRepository.save(client));
    }

    @Transactional
    public void delete(Long id) {
        Client client = findEntity(id);
        Address address = client.getAddress();

        clientRepository.delete(client);
        clientRepository.flush();
        addressRepository.delete(address);
        addressRepository.flush();
    }

    private void updateAddress(Client client, AddressRequestDTO address, City city) {
        client.getAddress().update(
                address.street(),
                address.number(),
                address.complement(),
                city);
    }

    private void validateDocuments(String cpf, String cnpj, Long clientId) {
        boolean hasCpf = cpf != null;
        boolean hasCnpj = cnpj != null;

        if (hasCpf == hasCnpj) {
            throw new ClientValidationException("Informe somente CPF ou CNPJ para o cliente.");
        }

        if (hasCpf) {
            validateCpf(cpf);

            if (clientRepository.existsByCpfAndIdNot(cpf, clientId)) {
                throw new ClientValidationException("CPF já cadastrado: " + cpf);
            }
        }

        if (hasCnpj) {
            validateCnpj(cnpj);

            if (clientRepository.existsByCnpjAndIdNot(cnpj, clientId)) {
                throw new ClientValidationException("CNPJ já cadastrado: " + cnpj);
            }
        }
    }

    private static void validateCpf(String cpf) {
        if (cpf.length() != 11 || cpf.chars().distinct().count() == 1) {
            throw new ClientValidationException("CPF inválido.");
        }

        int[] digits = cpf.chars().map(character -> character - '0').toArray();
        int firstCheckDigit = calculateCheckDigit(digits, 9, 10);
        int secondCheckDigit = calculateCheckDigit(digits, 10, 11);

        if (digits[9] != firstCheckDigit || digits[10] != secondCheckDigit) {
            throw new ClientValidationException("CPF inválido.");
        }
    }

    private static int calculateCheckDigit(int[] digits, int length, int initialWeight) {
        int sum = 0;

        for (int index = 0; index < length; index++) {
            sum += digits[index] * (initialWeight - index);
        }

        int checkDigit = 11 - (sum % 11);
        return checkDigit >= 10 ? 0 : checkDigit;
    }

    private static void validateCnpj(String cnpj) {
        if (cnpj.length() != 14 || cnpj.chars().distinct().count() == 1) {
            throw new ClientValidationException("CNPJ inválido.");
        }

        int[] digits = cnpj.chars().map(character -> character - '0').toArray();
        int firstCheckDigit = calculateCnpjCheckDigit(digits, 12, new int[]{5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
        int secondCheckDigit = calculateCnpjCheckDigit(digits, 13, new int[]{6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});

        if (digits[12] != firstCheckDigit || digits[13] != secondCheckDigit) {
            throw new ClientValidationException("CNPJ inválido.");
        }
    }

    private static int calculateCnpjCheckDigit(int[] digits, int length, int[] weights) {
        int sum = 0;

        for (int index = 0; index < length; index++) {
            sum += digits[index] * weights[index];
        }

        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    private Client findEntity(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new ClientNotFoundException(id));
    }
}
