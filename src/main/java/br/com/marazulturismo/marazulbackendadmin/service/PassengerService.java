package br.com.marazulturismo.marazulbackendadmin.service;

import br.com.marazulturismo.marazulbackendadmin.dto.PassengerRequestDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.PassengerResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.exception.PassengerNotFoundException;
import br.com.marazulturismo.marazulbackendadmin.exception.PassengerValidationException;
import br.com.marazulturismo.marazulbackendadmin.model.Passenger;
import br.com.marazulturismo.marazulbackendadmin.repository.PassengerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PassengerService {

    private final PassengerRepository passengerRepository;

    public PassengerService(PassengerRepository passengerRepository) {
        this.passengerRepository = passengerRepository;
    }

    @Transactional(readOnly = true)
    public List<PassengerResponseDTO> list(String search) {
        List<Passenger> passengers;

        if (search == null || search.isBlank()) {
            passengers = passengerRepository.findAllByOrderByNameAsc();
        } else {
            String digitsOnly = search.replaceAll("\\D", "");
            passengers = passengerRepository.searchByNameOrDocument(search.trim(), digitsOnly);
        }

        return passengers.stream()
                .map(PassengerResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public PassengerResponseDTO findById(Long id) {
        return PassengerResponseDTO.fromEntity(findEntity(id));
    }

    @Transactional
    public PassengerResponseDTO create(PassengerRequestDTO dto) {
        String cpf = Passenger.normalizeDocument(dto.cpf());
        validateCpf(cpf, null);

        Passenger passenger = new Passenger(dto.name().trim(), cpf, dto.phone());

        return PassengerResponseDTO.fromEntity(passengerRepository.save(passenger));
    }

    @Transactional
    public PassengerResponseDTO update(Long id, PassengerRequestDTO dto) {
        Passenger passenger = findEntity(id);
        String cpf = Passenger.normalizeDocument(dto.cpf());
        validateCpf(cpf, id);

        passenger.update(dto.name().trim(), cpf, dto.phone());

        return PassengerResponseDTO.fromEntity(passengerRepository.save(passenger));
    }

    @Transactional
    public void delete(Long id) {
        passengerRepository.delete(findEntity(id));
    }

    private void validateCpf(String cpf, Long passengerId) {
        if (cpf == null) {
            throw new PassengerValidationException("O CPF é obrigatório.");
        }

        boolean exists = passengerId == null
                ? passengerRepository.existsByCpf(cpf)
                : passengerRepository.existsByCpfAndIdNot(cpf, passengerId);

        if (exists) {
            throw new PassengerValidationException("CPF já cadastrado: " + cpf);
        }
    }

    private Passenger findEntity(Long id) {
        return passengerRepository.findById(id)
                .orElseThrow(() -> new PassengerNotFoundException(id));
    }
}
