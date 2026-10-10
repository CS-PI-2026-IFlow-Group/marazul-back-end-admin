package br.com.marazulturismo.marazulbackendadmin.repository;

import br.com.marazulturismo.marazulbackendadmin.model.Passenger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PassengerRepository extends JpaRepository<Passenger, Long> {

    boolean existsByCpf(String cpf);

    boolean existsByCpfAndIdNot(String cpf, Long id);

    List<Passenger> findAllByOrderByNameAsc();

    @Query("""
            SELECT passenger
            FROM Passenger passenger
            WHERE UPPER(passenger.name) LIKE UPPER(CONCAT('%', :search, '%'))
               OR (:digitsOnly IS NOT NULL AND :digitsOnly != ''
                   AND passenger.cpf LIKE CONCAT('%', :digitsOnly, '%'))
            ORDER BY passenger.name ASC
            """)
    List<Passenger> searchByNameOrDocument(@Param("search") String search,
                                           @Param("digitsOnly") String digitsOnly);
}
