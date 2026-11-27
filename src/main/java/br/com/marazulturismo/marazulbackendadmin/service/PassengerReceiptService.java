package br.com.marazulturismo.marazulbackendadmin.service;

import br.com.marazulturismo.marazulbackendadmin.dto.PassengerReceiptResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.exception.PassengerNotFoundException;
import br.com.marazulturismo.marazulbackendadmin.exception.TripNotFoundException;
import br.com.marazulturismo.marazulbackendadmin.exception.TripValidationException;
import br.com.marazulturismo.marazulbackendadmin.model.Trip;
import br.com.marazulturismo.marazulbackendadmin.model.TripPassenger;
import br.com.marazulturismo.marazulbackendadmin.repository.TripRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.TripPassengerRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PassengerReceiptService {

    private final TripRepository tripRepository;
    private final TripPassengerRepository tripPassengerRepository;
    private final String companyName;

    public PassengerReceiptService(TripRepository tripRepository,
                                   TripPassengerRepository tripPassengerRepository,
                                   @Value("${app.report.company-name:Marazul Turismo}") String companyName) {
        this.tripRepository = tripRepository;
        this.tripPassengerRepository = tripPassengerRepository;
        this.companyName = companyName;
    }

    @Transactional(readOnly = true)
    public PassengerReceiptResponseDTO generate(Long tripId, Long passengerId) {
        Trip trip = tripRepository.findForReceiptById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));
        TripPassenger payment = tripPassengerRepository.findForReceipt(tripId, passengerId)
                .orElseThrow(() -> new PassengerNotFoundException(passengerId));

        if (payment.getAmountPaid() == null || payment.getAmountPaid().signum() <= 0) {
            throw new TripValidationException(
                    "O passageiro não possui valor pago nesta viagem, por isso o recibo não pode ser emitido.");
        }

        return receipt(trip, payment, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public List<PassengerReceiptResponseDTO> generateAll(Long tripId) {
        Trip trip = tripRepository.findForReceiptById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));
        LocalDateTime issuedAt = LocalDateTime.now();
        List<PassengerReceiptResponseDTO> receipts = tripPassengerRepository.findForReceiptsByTripId(tripId).stream()
                .filter(payment -> payment.getAmountPaid() != null && payment.getAmountPaid().signum() > 0)
                .map(payment -> receipt(trip, payment, issuedAt))
                .toList();
        if (receipts.isEmpty()) {
            throw new TripValidationException(
                    "A viagem não possui passageiros com valor pago, por isso os recibos não podem ser emitidos.");
        }
        return receipts;
    }

    private PassengerReceiptResponseDTO receipt(
            Trip trip, TripPassenger payment, LocalDateTime issuedAt) {
        return new PassengerReceiptResponseDTO(
                "REC-" + trip.getId() + "-" + payment.getPassenger().getId(),
                trip.getId(),
                payment.getPassenger().getId(),
                payment.getPassenger().getName(),
                payment.getPassenger().getCpf(),
                payment.getAmountPaid(),
                MoneyInWords.convert(payment.getAmountPaid()),
                trip.getOriginCity().getName(),
                trip.getOriginCity().getState().getAcronym(),
                trip.getDestinationCity().getName(),
                trip.getDestinationCity().getState().getAcronym(),
                trip.getPlannedDepartureOrigin(),
                companyName,
                issuedAt);
    }
}
