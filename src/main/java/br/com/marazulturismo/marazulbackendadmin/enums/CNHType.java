package br.com.marazulturismo.marazulbackendadmin.enums;

public enum CNHType {

    A("A"),
    B("B"),
    AB("AB"),
    C("C"),
    D("D"),
    E("E");

    private final String label;

    CNHType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
