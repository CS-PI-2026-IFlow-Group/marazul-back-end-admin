package br.com.marazulturismo.marazulbackendadmin.enums;

/**
 * Modelos de carroceria aceitos pela operação.
 * Persistido como texto (EnumType.STRING), impedindo valores livres.
 */
public enum ModeloCarroceria {

    MARCOPOLO("Marcopolo"),
    COMIL("Comil"),
    IRIZAR_BRASIL("Irizar Brasil"),
    BUSSCAR("Busscar");

    private final String descricao;

    ModeloCarroceria(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
