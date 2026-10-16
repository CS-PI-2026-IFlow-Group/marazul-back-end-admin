package br.com.marazulturismo.marazulbackendadmin.security;

import java.security.Principal;

public record AuthenticatedUser(Long id, String email) implements Principal {

    @Override
    public String getName() {
        return email;
    }
}
