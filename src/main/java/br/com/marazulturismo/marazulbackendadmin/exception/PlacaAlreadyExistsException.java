package br.com.marazulturismo.marazulbackendadmin.exception;

public class PlacaAlreadyExistsException extends RuntimeException {
    public PlacaAlreadyExistsException(String placa) {
        super("Placa já cadastrada: " + placa);
    }
}
