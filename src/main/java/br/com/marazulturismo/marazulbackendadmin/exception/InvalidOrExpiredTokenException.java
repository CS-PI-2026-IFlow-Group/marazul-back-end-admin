package br.com.marazulturismo.marazulbackendadmin.exception;

public class InvalidOrExpiredTokenException extends RuntimeException {
    public InvalidOrExpiredTokenException() {
        super("Token inválido ou expirado.");
    }
}
