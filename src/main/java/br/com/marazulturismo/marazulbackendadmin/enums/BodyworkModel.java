package br.com.marazulturismo.marazulbackendadmin.enums;

public enum BodyworkModel {

    MARCOPOLO("Marcopolo"),
    COMIL("Comil"),
    IRIZAR_BRASIL("Irizar Brasil"),
    BUSSCAR("Busscar");

    private final String label;

    BodyworkModel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
