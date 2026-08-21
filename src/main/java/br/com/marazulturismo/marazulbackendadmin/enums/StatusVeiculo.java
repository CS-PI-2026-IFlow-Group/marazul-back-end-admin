package br.com.marazulturismo.marazulbackendadmin.enums;

/**
 * Situação do veículo na frota. ATIVO é o padrão no cadastro.
 */
public enum StatusVeiculo {

    ATIVO("Ativo"),
    INATIVO("Inativo"),
    EM_MANUTENCAO("Em manutenção");

    private final String descricao;

    StatusVeiculo(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
