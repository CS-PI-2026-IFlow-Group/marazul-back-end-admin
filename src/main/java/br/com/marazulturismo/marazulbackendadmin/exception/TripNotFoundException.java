package br.com.marazulturismo.marazulbackendadmin.exception;

public class TripNotFoundException extends RuntimeException {
    public TripNotFoundException(Long id) {
        super("Viagem não encontrada: " + id);
    }
}
