package br.com.marazulturismo.marazulbackendadmin.exception;

public class PassengerNotFoundException extends RuntimeException {
    public PassengerNotFoundException(Long id) {
        super("Passageiro não encontrado: " + id);
    }
}
