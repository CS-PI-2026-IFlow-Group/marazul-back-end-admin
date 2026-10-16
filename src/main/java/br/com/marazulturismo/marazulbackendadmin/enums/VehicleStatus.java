package br.com.marazulturismo.marazulbackendadmin.enums;

public enum VehicleStatus {

    ACTIVE("Ativo"),
    INACTIVE("Inativo"),
    UNDER_MAINTENANCE("Em manutenção");

    private final String label;

    VehicleStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
