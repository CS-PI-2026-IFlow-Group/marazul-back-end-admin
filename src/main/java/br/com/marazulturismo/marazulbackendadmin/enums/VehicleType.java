package br.com.marazulturismo.marazulbackendadmin.enums;

public enum VehicleType {

    DD("DD"),
    LD("LD"),
    CONVENTIONAL("Convencional");

    private final String label;

    VehicleType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
