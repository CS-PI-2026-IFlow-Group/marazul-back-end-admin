package br.com.marazulturismo.marazulbackendadmin.exception;

public class AddressNotFoundException extends RuntimeException {

    public AddressNotFoundException(Long id) {
        super("Endereço não encontrado: " + id);
    }
}
