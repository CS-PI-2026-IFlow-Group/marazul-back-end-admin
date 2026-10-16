package br.com.marazulturismo.marazulbackendadmin.exception;

public class VehicleNotFoundException extends RuntimeException {
    public VehicleNotFoundException(Long id) {
        super("Veículo não encontrado: " + id);
    }
}
