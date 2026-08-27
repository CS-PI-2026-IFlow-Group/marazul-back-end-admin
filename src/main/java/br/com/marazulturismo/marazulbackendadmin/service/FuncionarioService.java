package br.com.marazulturismo.marazulbackendadmin.service;

import br.com.marazulturismo.marazulbackendadmin.dto.FuncionarioRequestDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.FuncionarioResponseDTO;
import br.com.marazulturismo.marazulbackendadmin.enums.Position;
import br.com.marazulturismo.marazulbackendadmin.enums.StatusFuncionario;
import br.com.marazulturismo.marazulbackendadmin.enums.UserRole;
import br.com.marazulturismo.marazulbackendadmin.exception.EmailAlreadyExistsException;
import br.com.marazulturismo.marazulbackendadmin.exception.FuncionarioNotFoundException;
import br.com.marazulturismo.marazulbackendadmin.exception.FuncionarioValidationException;
import br.com.marazulturismo.marazulbackendadmin.model.CNH;
import br.com.marazulturismo.marazulbackendadmin.model.User;
import br.com.marazulturismo.marazulbackendadmin.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class FuncionarioService {

    private final UserRepository userRepository;

    public FuncionarioService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<FuncionarioResponseDTO> listar(Position funcao, StatusFuncionario status) {
        return userRepository.findAll().stream()
                .filter(user -> funcao == null || user.getPosition() == funcao)
                .filter(user -> status == null || statusDo(user) == status)
                .map(FuncionarioResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public FuncionarioResponseDTO buscarPorId(Long id) {
        return FuncionarioResponseDTO.fromEntity(buscarEntidade(id));
    }

    @Transactional
    public FuncionarioResponseDTO cadastrar(FuncionarioRequestDTO dto) {
        validarEmailDisponivel(dto.email(), null);
        Position position = posicaoValida(dto.position());
        CNH cnh = cnhValida(dto, position);

        User funcionario = new User(
                dto.name(),
                new Date(),
                position,
                UserRole.USER,
                dto.email(),
                dto.cellphoneNumber(),
                cnh,
                null
        );

        return FuncionarioResponseDTO.fromEntity(userRepository.save(funcionario));
    }

    @Transactional
    public FuncionarioResponseDTO atualizar(Long id, FuncionarioRequestDTO dto) {
        User funcionario = buscarEntidade(id);
        validarEmailDisponivel(dto.email(), id);
        Position position = posicaoValida(dto.position());
        CNH cnh = cnhValida(dto, position);

        funcionario.atualizar(dto.name(), dto.email(), dto.cellphoneNumber(), position, cnh);
        return FuncionarioResponseDTO.fromEntity(userRepository.save(funcionario));
    }

    @Transactional
    public void excluir(Long id) {
        User funcionario = buscarEntidade(id);
        funcionario.inativar();
        userRepository.save(funcionario);
    }

    private User buscarEntidade(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new FuncionarioNotFoundException(id));
    }

    private void validarEmailDisponivel(String email, Long id) {
        if (email == null) {
            return;
        }

        userRepository.findByEmail(email)
                .filter(user -> !user.getId().equals(id))
                .ifPresent(user -> {
                    throw new EmailAlreadyExistsException(email);
                });
    }

    private static Position posicaoValida(Position position) {
        return position == Position.DRIVER ? Position.DRIVER : Position.OTHER;
    }

    private static CNH cnhValida(FuncionarioRequestDTO dto, Position position) {
        if (position != Position.DRIVER) {
            return null;
        }

        if (dto.cnhNumber() == null || dto.cnhNumber().isBlank() || dto.cnhType() == null) {
            throw new FuncionarioValidationException(
                    "CNH e categoria da CNH são obrigatórias para funcionários motoristas.");
        }

        return new CNH(dto.cnhNumber(), dto.cnhType());
    }

    private static StatusFuncionario statusDo(User user) {
        return user.isInativo() ? StatusFuncionario.INATIVO : StatusFuncionario.ATIVO;
    }
}
