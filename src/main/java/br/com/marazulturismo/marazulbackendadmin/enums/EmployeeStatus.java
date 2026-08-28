package br.com.marazulturismo.marazulbackendadmin.enums;

public enum EmployeeStatus {

    ACTIVE("Ativo"),
    INACTIVE("Inativo");

    private final String label;

    EmployeeStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
