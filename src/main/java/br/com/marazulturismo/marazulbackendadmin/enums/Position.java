package br.com.marazulturismo.marazulbackendadmin.enums;

public enum Position {

    DRIVER("Motorista"),
    OTHER("Outros");

    private final String label;

    Position(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
