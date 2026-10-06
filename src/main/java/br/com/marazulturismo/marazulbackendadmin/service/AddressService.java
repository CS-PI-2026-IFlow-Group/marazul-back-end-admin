package br.com.marazulturismo.marazulbackendadmin.service;

import br.com.marazulturismo.marazulbackendadmin.dto.AddressResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.exception.AddressNotFoundException;
import br.com.marazulturismo.marazulbackendadmin.model.Address;
import br.com.marazulturismo.marazulbackendadmin.repository.AddressRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.ClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AddressService {

    private final AddressRepository addressRepository;
    private final ClientRepository clientRepository;

    public AddressService(AddressRepository addressRepository, ClientRepository clientRepository) {
        this.addressRepository = addressRepository;
        this.clientRepository = clientRepository;
    }

    @Transactional(readOnly = true)
    public List<AddressResponseDTO> list() {
        return addressRepository.findAllByOrderByIdAsc().stream()
                .map(address -> AddressResponseDTO.fromEntity(
                        address,
                        clientRepository.findByAddressId(address.getId()).orElse(null)))
                .toList();
    }

    @Transactional(readOnly = true)
    public AddressResponseDTO findById(Long id) {
        Address address = findEntity(id);

        return AddressResponseDTO.fromEntity(
                address,
                clientRepository.findByAddressId(address.getId()).orElse(null));
    }

    private Address findEntity(Long id) {
        return addressRepository.findById(id)
                .orElseThrow(() -> new AddressNotFoundException(id));
    }
}
