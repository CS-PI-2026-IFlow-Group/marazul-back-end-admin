package br.com.marazulturismo.marazulbackendadmin.enums;

/**
 * Tipo de operação do veículo.
 * DD = Double Decker, LD = Leito Duplo, CONVENCIONAL = convencional.
 */
public enum TipoVeiculo {

    DD("DD"),
    LD("LD"),
    CONVENCIONAL("Convencional");

    private final String descricao;

    TipoVeiculo(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
