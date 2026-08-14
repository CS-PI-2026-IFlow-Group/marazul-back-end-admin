package br.com.marazulturismo.marazulbackendadmin.service;

import br.com.marazulturismo.marazulbackendadmin.dto.VeiculoRequestDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.VeiculoResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.enums.ModeloCarroceria;
import br.com.marazulturismo.marazulbackendadmin.enums.StatusVeiculo;
import br.com.marazulturismo.marazulbackendadmin.enums.TipoVeiculo;
import br.com.marazulturismo.marazulbackendadmin.exception.PlacaAlreadyExistsException;
import br.com.marazulturismo.marazulbackendadmin.exception.VeiculoNotFoundException;
import br.com.marazulturismo.marazulbackendadmin.model.Veiculo;
import br.com.marazulturismo.marazulbackendadmin.repository.VeiculoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FrotaServiceTest {

    @Mock
    private VeiculoRepository veiculoRepository;

    @InjectMocks
    private FrotaService frotaService;

    private VeiculoRequestDTO requestSemStatus() {
        return new VeiculoRequestDTO(
                "1001",
                "ABC1D23",
                "Mercedes-Benz",
                ModeloCarroceria.MARCOPOLLO,
                TipoVeiculo.LD,
                2022,
                46,
                null,
                null
        );
    }

    @Test
    void cadastrar_atribuiStatusAtivoQuandoNaoInformado() {
        when(veiculoRepository.existsByPlaca("ABC1D23")).thenReturn(false);
        when(veiculoRepository.save(any(Veiculo.class))).thenAnswer(inv -> inv.getArgument(0));

        VeiculoResponseDTO response = frotaService.cadastrar(requestSemStatus());

        assertThat(response.status()).isEqualTo(StatusVeiculo.ATIVO);

        ArgumentCaptor<Veiculo> captor = ArgumentCaptor.forClass(Veiculo.class);
        verify(veiculoRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(StatusVeiculo.ATIVO);
    }

    @Test
    void cadastrar_persistePlacaNormalizada() {
        when(veiculoRepository.existsByPlaca("ABC1D23")).thenReturn(false);
        when(veiculoRepository.save(any(Veiculo.class))).thenAnswer(inv -> inv.getArgument(0));

        VeiculoRequestDTO dto = new VeiculoRequestDTO(
                "1001", "  abc1d23 ", "Mercedes-Benz",
                ModeloCarroceria.MARCOPOLLO, TipoVeiculo.LD,
                2022, 46, null, null);

        VeiculoResponseDTO response = frotaService.cadastrar(dto);

        assertThat(response.placa()).isEqualTo("ABC1D23");
    }

    @Test
    void cadastrar_rejeitaPlacaDuplicada() {
        when(veiculoRepository.existsByPlaca("ABC1D23")).thenReturn(true);

        assertThatThrownBy(() -> frotaService.cadastrar(requestSemStatus()))
                .isInstanceOf(PlacaAlreadyExistsException.class);

        verify(veiculoRepository, never()).save(any());
    }

    @Test
    void excluir_realizaInativacaoLogica() {
        Veiculo veiculo = new Veiculo(
                "1001", "ABC1D23", "Volvo",
                ModeloCarroceria.BUSSCAR, TipoVeiculo.DD,
                2021, 50, null, StatusVeiculo.ATIVO);
        when(veiculoRepository.findById(1L)).thenReturn(Optional.of(veiculo));

        frotaService.excluir(1L);

        assertThat(veiculo.getStatus()).isEqualTo(StatusVeiculo.INATIVO);
        verify(veiculoRepository).save(veiculo);
    }

    @Test
    void buscarPorId_lancaNotFoundQuandoInexistente() {
        when(veiculoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> frotaService.buscarPorId(99L))
                .isInstanceOf(VeiculoNotFoundException.class);
    }

    @Test
    void atualizar_rejeitaPlacaDeOutroVeiculo() {
        Veiculo veiculo = new Veiculo(
                "1001", "ABC1D23", "Volvo",
                ModeloCarroceria.COMIL, TipoVeiculo.CONVENCIONAL,
                2020, 44, null, StatusVeiculo.ATIVO);
        when(veiculoRepository.findById(1L)).thenReturn(Optional.of(veiculo));
        when(veiculoRepository.existsByPlacaAndIdNot("XYZ9K88", 1L)).thenReturn(true);

        VeiculoRequestDTO dto = new VeiculoRequestDTO(
                "1001", "XYZ9K88", "Volvo",
                ModeloCarroceria.COMIL, TipoVeiculo.CONVENCIONAL,
                2020, 44, null, StatusVeiculo.ATIVO);

        assertThatThrownBy(() -> frotaService.atualizar(1L, dto))
                .isInstanceOf(PlacaAlreadyExistsException.class);

        verify(veiculoRepository, never()).save(any());
    }
}
