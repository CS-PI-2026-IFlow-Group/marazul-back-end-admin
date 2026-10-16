package br.com.marazulturismo.marazulbackendadmin.exception;

public class LicensePlateAlreadyExistsException extends RuntimeException {
    public LicensePlateAlreadyExistsException(String licensePlate) {
        super("Placa já cadastrada: " + licensePlate);
    }
}
