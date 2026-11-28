package br.com.marazulturismo.marazulbackendadmin.service;

import br.com.marazulturismo.marazulbackendadmin.dto.TripResultDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.TripResultExpenseDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.TripResultResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.exception.TripNotFoundException;
import br.com.marazulturismo.marazulbackendadmin.repository.ExpenseRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.TripResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TripResultService {

    private final TripResultRepository tripResultRepository;
    private final ExpenseRepository expenseRepository;

    public TripResultService(TripResultRepository tripResultRepository, ExpenseRepository expenseRepository) {
        this.tripResultRepository = tripResultRepository;
        this.expenseRepository = expenseRepository;
    }

    @Transactional(readOnly = true)
    public TripResultResponseDTO generate(Long tripId) {
        TripResultDTO result = tripResultRepository.findResultByTripId(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));

        List<TripResultExpenseDTO> expenses = expenseRepository.findByTripIdOrderByIdAsc(tripId).stream()
                .map(expense -> new TripResultExpenseDTO(
                        expense.getDescription(), TripResultDTO.money(expense.getAmount())))
                .toList();

        return new TripResultResponseDTO(
                result.tripId(),
                result.route(),
                result.plannedDepartureOrigin(),
                result.client(),
                result.status().getLabel(),
                result.charged(),
                result.received(),
                result.expenses(),
                result.result(),
                expenses,
                LocalDateTime.now());
    }
}
