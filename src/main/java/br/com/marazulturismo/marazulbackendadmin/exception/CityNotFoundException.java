package br.com.marazulturismo.marazulbackendadmin.exception;

public class CityNotFoundException extends RuntimeException {
    public CityNotFoundException(Long id) {
        super("Cidade não encontrada: " + id + ".");
    }
    
}
