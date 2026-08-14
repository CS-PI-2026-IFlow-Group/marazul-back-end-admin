package br.com.marazulturismo.marazulbackendadmin.controller;

import br.com.marazulturismo.marazulbackendadmin.dto.VeiculoRequestDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.VeiculoResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.enums.StatusVeiculo;
import br.com.marazulturismo.marazulbackendadmin.service.FrotaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/frota")
public class FrotaController {

    private final FrotaService frotaService;

    public FrotaController(FrotaService frotaService) {
        this.frotaService = frotaService;
    }

    /**
     * @param status filtro opcional. Ausente, retorna a frota inteira
     *               (inclusive veículos inativados, que seguem consultáveis).
     *               Informado, restringe ao status pedido — o front usa
     *               {@code ?status=ATIVO} para montar a seleção de novas viagens.
     */
    @GetMapping
    public ResponseEntity<List<VeiculoResponseDTO>> listar(
            @RequestParam(required = false) StatusVeiculo status) {
        return ResponseEntity.ok(frotaService.listar(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VeiculoResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(frotaService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<VeiculoResponseDTO> cadastrar(@RequestBody @Valid VeiculoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(frotaService.cadastrar(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VeiculoResponseDTO> atualizar(@PathVariable Long id,
                                                        @RequestBody @Valid VeiculoRequestDTO dto) {
        return ResponseEntity.ok(frotaService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        frotaService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
