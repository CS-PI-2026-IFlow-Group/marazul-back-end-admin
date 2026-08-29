package br.com.marazulturismo.marazulbackendadmin.enums;

public enum UserRole {
    ADMIN("Administrador (Admin)"),
    USER("Comum"),
    NO_ACCESS("Sem Acesso");

    private final String label;

    UserRole(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

