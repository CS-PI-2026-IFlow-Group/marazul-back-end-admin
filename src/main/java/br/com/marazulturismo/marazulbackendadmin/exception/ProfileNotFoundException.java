package br.com.marazulturismo.marazulbackendadmin.exception;

public class ProfileNotFoundException extends RuntimeException {
    public ProfileNotFoundException(Long id) {
        super("Perfil não encontrado: " + id);
    }
}
