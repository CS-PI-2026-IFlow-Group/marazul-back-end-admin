package br.com.marazulturismo.marazulbackendadmin.dto;

/**
 * Opção de um campo enumerado. {@code valor} é o que a API espera receber de
 * volta no cadastro; {@code descricao} é o rótulo legível para exibição.
 */
public record EnumOptionDTO(String valor, String descricao) {
}
