package br.com.marazulturismo.marazulbackendadmin.service;

import br.com.marazulturismo.marazulbackendadmin.dto.AddressResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.AddressRequestDTO;
import br.com.marazulturismo.marazulbackendadmin.exception.AddressNotFoundException;
import br.com.marazulturismo.marazulbackendadmin.exception.AddressValidationException;
import br.com.marazulturismo.marazulbackendadmin.model.Address;
import br.com.marazulturismo.marazulbackendadmin.model.City;
import br.com.marazulturismo.marazulbackendadmin.repository.AddressRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.CityRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.ClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AddressService {

    private final AddressRepository addressRepository;
    private final CityRepository cityRepository;
    private final ClientRepository clientRepository;

    public AddressService(
            AddressRepository addressRepository,
            CityRepository cityRepository,
            ClientRepository clientRepository) {
        this.addressRepository = addressRepository;
        this.cityRepository = cityRepository;
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

    @Transactional
    public AddressResponseDTO update(Long id, AddressRequestDTO dto) {
        Address address = findEntity(id);
        City city = cityRepository.findById(dto.cityId())
                .orElseThrow(() -> new AddressValidationException("Cidade não encontrada."));

        address.update(dto.street(), dto.number(), dto.complement(), city);
        Address updatedAddress = addressRepository.save(address);

        return AddressResponseDTO.fromEntity(
                updatedAddress,
                clientRepository.findByAddressId(updatedAddress.getId()).orElse(null));
    }

    @Transactional
    public void delete(Long id) {
        Address address = findEntity(id);

        if (clientRepository.findByAddressId(id).isPresent()) {
            throw new AddressValidationException(
                    "O endereço está vinculado a um cliente e deve ser excluído pelo cadastro do cliente.");
        }

        addressRepository.delete(address);
        addressRepository.flush();
    }

    private Address findEntity(Long id) {
        return addressRepository.findById(id)
                .orElseThrow(() -> new AddressNotFoundException(id));
    }
}
