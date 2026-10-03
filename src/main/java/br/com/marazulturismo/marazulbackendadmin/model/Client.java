package br.com.marazulturismo.marazulbackendadmin.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "clients")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String cpf;

    @Column(unique = true)
    private String cnpj;

    @Embedded
    private Address address;

    public Client(String name, String cpf, String cnpj, Address address) {
        this.name = name;
        this.cpf = cpf;
        this.cnpj = cnpj;
        this.address = address;
    }

    public void update(String name, String cpf, String cnpj, Address address) {
        this.name = name;
        this.cpf = cpf;
        this.cnpj = cnpj;
        this.address = address;
    }
}
