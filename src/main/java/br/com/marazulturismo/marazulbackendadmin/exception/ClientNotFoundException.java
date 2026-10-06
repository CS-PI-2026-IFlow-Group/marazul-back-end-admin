package br.com.marazulturismo.marazulbackendadmin.exception;

public class ClientNotFoundException extends RuntimeException {

    public ClientNotFoundException(Long id) {
        super("Cliente não encontrado: " + id);
    }
}
