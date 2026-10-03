package br.com.marazulturismo.marazulbackendadmin.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
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

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "address_id", nullable = false, unique = true)
    private Address address;

    public Client(String name, String cpf, String cnpj, Address address) {
        this.name = name;
        this.cpf = cpf;
        this.cnpj = cnpj;
        this.address = address;
    }

    @PrePersist
    @PreUpdate
    private void normalizeAndValidateDocuments() {
        cpf = normalizeDocument(cpf);
        cnpj = normalizeDocument(cnpj);

        if ((cpf == null) == (cnpj == null)) {
            throw new IllegalStateException("Informe somente CPF ou CNPJ para o cliente.");
        }
    }

    private static String normalizeDocument(String document) {
        if (document == null) {
            return null;
        }

        String digits = document.replaceAll("\\D", "");
        return digits.isEmpty() ? null : digits;
    }
}
