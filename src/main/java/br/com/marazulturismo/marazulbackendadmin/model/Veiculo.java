package br.com.marazulturismo.marazulbackendadmin.model;

import br.com.marazulturismo.marazulbackendadmin.enums.ModeloCarroceria;
import br.com.marazulturismo.marazulbackendadmin.enums.StatusVeiculo;
import br.com.marazulturismo.marazulbackendadmin.enums.TipoVeiculo;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "veiculos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Veiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String prefixo;

    @Column(nullable = false, unique = true)
    private String placa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModeloCarroceria modelo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoVeiculo tipo;

    @Column(nullable = false)
    private Integer ano;

    @Column(nullable = false)
    private Integer assentos;

    private LocalDate dataVistoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusVeiculo status;

    public Veiculo(
            String prefixo,
            String placa,
            ModeloCarroceria modelo,
            TipoVeiculo tipo,
            Integer ano,
            Integer assentos,
            LocalDate dataVistoria,
            StatusVeiculo status) {

        this.prefixo = prefixo;
        this.placa = placa;
        this.modelo = modelo;
        this.tipo = tipo;
        this.ano = ano;
        this.assentos = assentos;
        this.dataVistoria = dataVistoria;
        this.status = status != null ? status : StatusVeiculo.ATIVO;
    }

    public void atualizar(
            String prefixo,
            String placa,
            ModeloCarroceria modelo,
            TipoVeiculo tipo,
            Integer ano,
            Integer assentos,
            LocalDate dataVistoria,
            StatusVeiculo status) {

        this.prefixo = prefixo;
        this.placa = placa;
        this.modelo = modelo;
        this.tipo = tipo;
        this.ano = ano;
        this.assentos = assentos;
        this.dataVistoria = dataVistoria;
        if (status != null) {
            this.status = status;
        }
    }

    public void inativar() {
        this.status = StatusVeiculo.INATIVO;
    }

    public boolean isInativo() {
        return this.status == StatusVeiculo.INATIVO;
    }
}
