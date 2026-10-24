package br.com.marazulturismo.marazulbackendadmin.enums;

public enum TripStatus {
    SCHEDULED("Agendada"),
    CONFIRMED("Confirmada"),
    LEFT_ORIGIN("Saiu da origem"),
    ARRIVED_DESTINATION("Chegou ao destino"),
    LEFT_DESTINATION("Saiu do destino"),
    ARRIVED_ORIGIN("Chegou à origem"),
    COMPLETED("Concluída");

    private final String label;

    TripStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
