package br.com.marazulturismo.marazulbackendadmin.dto;

import br.com.marazulturismo.marazulbackendadmin.enums.ModeloCarroceria;
import br.com.marazulturismo.marazulbackendadmin.enums.StatusVeiculo;
import br.com.marazulturismo.marazulbackendadmin.enums.TipoVeiculo;
import br.com.marazulturismo.marazulbackendadmin.model.Veiculo;

import java.time.LocalDate;

public record VeiculoResponseDTO(
        Long id,
        String prefixo,
        String placa,
        String marca,
        ModeloCarroceria modelo,
        TipoVeiculo tipo,
        Integer ano,
        Integer assentos,
        LocalDate dataVistoria,
        StatusVeiculo status
) {
    public static VeiculoResponseDTO fromEntity(Veiculo veiculo) {
        return new VeiculoResponseDTO(
                veiculo.getId(),
                veiculo.getPrefixo(),
                veiculo.getPlaca(),
                veiculo.getMarca(),
                veiculo.getModelo(),
                veiculo.getTipo(),
                veiculo.getAno(),
                veiculo.getAssentos(),
                veiculo.getDataVistoria(),
                veiculo.getStatus()
        );
    }
}
