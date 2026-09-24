package br.com.marazulturismo.marazulbackendadmin.exception;

public class ProfileNotFoundException extends RuntimeException {

    public ProfileNotFoundException(Long id) {
        super("Perfil nao encontrado para o ID: " + id + ".");
    }
}
