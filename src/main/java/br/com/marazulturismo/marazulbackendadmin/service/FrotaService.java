package br.com.marazulturismo.marazulbackendadmin.service;

import br.com.marazulturismo.marazulbackendadmin.dto.VeiculoRequestDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.VeiculoResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.enums.StatusVeiculo;
import br.com.marazulturismo.marazulbackendadmin.exception.PlacaAlreadyExistsException;
import br.com.marazulturismo.marazulbackendadmin.exception.VeiculoNotFoundException;
import br.com.marazulturismo.marazulbackendadmin.model.Veiculo;
import br.com.marazulturismo.marazulbackendadmin.repository.VeiculoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class FrotaService {

    private final VeiculoRepository veiculoRepository;

    public FrotaService(VeiculoRepository veiculoRepository) {
        this.veiculoRepository = veiculoRepository;
    }

    /**
     * Lista a frota. Sem filtro retorna todos os veículos, inclusive os
     * inativados, que permanecem consultáveis. Com filtro de status o
     * front-end obtém apenas os veículos elegíveis para novas viagens
     * (status ATIVO).
     */
    @Transactional(readOnly = true)
    public List<VeiculoResponseDTO> listar(StatusVeiculo status) {
        List<Veiculo> veiculos = status == null
                ? veiculoRepository.findAll()
                : veiculoRepository.findByStatus(status);

        return veiculos.stream()
                .map(VeiculoResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public VeiculoResponseDTO buscarPorId(Long id) {
        return VeiculoResponseDTO.fromEntity(buscarEntidade(id));
    }

    @Transactional
    public VeiculoResponseDTO cadastrar(VeiculoRequestDTO dto) {
        String placa = normalizarPlaca(dto.placa());

        if (veiculoRepository.existsByPlaca(placa)) {
            throw new PlacaAlreadyExistsException(placa);
        }

        Veiculo veiculo = new Veiculo(
                dto.prefixo(),
                placa,
                dto.marca(),
                dto.modelo(),
                dto.tipo(),
                dto.ano(),
                dto.assentos(),
                dto.dataVistoria(),
                dto.status()
        );

        return VeiculoResponseDTO.fromEntity(veiculoRepository.save(veiculo));
    }

    @Transactional
    public VeiculoResponseDTO atualizar(Long id, VeiculoRequestDTO dto) {
        Veiculo veiculo = buscarEntidade(id);
        String placa = normalizarPlaca(dto.placa());

        if (veiculoRepository.existsByPlacaAndIdNot(placa, id)) {
            throw new PlacaAlreadyExistsException(placa);
        }

        veiculo.atualizar(
                dto.prefixo(),
                placa,
                dto.marca(),
                dto.modelo(),
                dto.tipo(),
                dto.ano(),
                dto.assentos(),
                dto.dataVistoria(),
                dto.status()
        );

        return VeiculoResponseDTO.fromEntity(veiculoRepository.save(veiculo));
    }

    /**
     * Exclusão lógica: em vez de remover fisicamente, o veículo é inativado
     * (status INATIVO), preservando o histórico e mantendo o registro
     * consultável. Assim ele deixa de aparecer na seleção de novas viagens,
     * mas as viagens já vinculadas continuam íntegras.
     *
     * <p>Quando a entidade Viagem existir, este é o ponto onde a verificação
     * de vínculo deve entrar para permitir exclusão física dos veículos que
     * nunca rodaram.
     */
    @Transactional
    public void excluir(Long id) {
        Veiculo veiculo = buscarEntidade(id);
        veiculo.inativar();
        veiculoRepository.save(veiculo);
    }

    private Veiculo buscarEntidade(Long id) {
        return veiculoRepository.findById(id)
                .orElseThrow(() -> new VeiculoNotFoundException(id));
    }

    /**
     * A unicidade da placa só se sustenta se a comparação for feita sobre um
     * valor canônico: sem espaços nas bordas e em caixa alta.
     */
    private static String normalizarPlaca(String placa) {
        return placa.trim().toUpperCase(Locale.ROOT);
    }
}
