package br.com.marazulturismo.marazulbackendadmin.dto;

import br.com.marazulturismo.marazulbackendadmin.enums.ModeloCarroceria;
import br.com.marazulturismo.marazulbackendadmin.enums.StatusVeiculo;
import br.com.marazulturismo.marazulbackendadmin.enums.TipoVeiculo;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

/**
 * Dados de entrada para cadastro/atualização de veículos.
 * prefixo, placa, marca, modelo, tipo, ano e assentos são obrigatórios.
 * dataVistoria e status são opcionais (status assume ATIVO quando ausente).
 */
public record VeiculoRequestDTO(

        @NotBlank(message = "O prefixo é obrigatório.")
        String prefixo,

        @NotBlank(message = "A placa é obrigatória.")
        @Pattern(
                regexp = "^[A-Za-z]{3}[0-9][A-Za-z0-9][0-9]{2}$",
                message = "A placa deve estar no formato ABC1234 ou ABC1D23.")
        String placa,

        @NotBlank(message = "A marca é obrigatória.")
        String marca,

        @NotNull(message = "O modelo de carroceria é obrigatório.")
        ModeloCarroceria modelo,

        @NotNull(message = "O tipo é obrigatório.")
        TipoVeiculo tipo,

        @NotNull(message = "O ano é obrigatório.")
        @Min(value = 1950, message = "O ano deve ser igual ou posterior a 1950.")
        Integer ano,

        @NotNull(message = "A quantidade de assentos é obrigatória.")
        @Positive(message = "A quantidade de assentos deve ser maior que zero.")
        Integer assentos,

        LocalDate dataVistoria,

        StatusVeiculo status
) {}
