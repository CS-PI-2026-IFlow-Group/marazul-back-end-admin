package br.com.marazulturismo.marazulbackendadmin.repository;

import br.com.marazulturismo.marazulbackendadmin.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClientRepository extends JpaRepository<Client, Long> {

    boolean existsByCpf(String cpf);

    boolean existsByCnpj(String cnpj);

    boolean existsByCpfAndIdNot(String cpf, Long id);

    boolean existsByCnpjAndIdNot(String cnpj, Long id);

    List<Client> findByNameContainingIgnoreCase(String name);
}
