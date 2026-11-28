package br.com.marazulturismo.marazulbackendadmin.security;

import java.security.Principal;

public record AuthenticatedUser(Long id, String email, String profile) implements Principal {

    @Override
    public String getName() {
        return email;
    }
}
