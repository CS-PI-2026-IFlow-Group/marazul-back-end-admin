package br.com.marazulturismo.marazulbackendadmin.exception;

public class ClientValidationException extends RuntimeException {
    public ClientValidationException(String message) {
        super(message);
    }
}
