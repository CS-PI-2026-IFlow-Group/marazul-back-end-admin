package br.com.marazulturismo.marazulbackendadmin.repository;

import br.com.marazulturismo.marazulbackendadmin.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClientRepository extends JpaRepository<Client, Long> {

    boolean existsByCpf(String cpf);

    boolean existsByCnpj(String cnpj);

    boolean existsByCpfAndIdNot(String cpf, Long id);

    boolean existsByCnpjAndIdNot(String cnpj, Long id);

    @Query("""
            SELECT c FROM Client c
            WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
               OR (:digitsOnly IS NOT NULL AND :digitsOnly != '' AND (c.cpf LIKE CONCAT('%', :digitsOnly, '%') OR c.cnpj LIKE CONCAT('%', :digitsOnly, '%')))
            """)
    List<Client> searchByNameOrDocument(
            @Param("search") String search,
            @Param("digitsOnly") String digitsOnly);
}
