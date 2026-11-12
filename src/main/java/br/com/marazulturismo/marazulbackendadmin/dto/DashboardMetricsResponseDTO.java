package br.com.marazulturismo.marazulbackendadmin.dto;

public record DashboardMetricsResponseDTO(
        Long totalVehicles,
        Long activeEmployees,
        Long totalClients,
        Long totalPassengers
) {
}
