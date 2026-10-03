package br.com.marazulturismo.marazulbackendadmin.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Locale;

@Entity
@Table(name = "passengers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Passenger {

    private static final String DOCUMENT_SEPARATORS = "[\\s./-]";

    private static final String NON_DIGITS = "\\D";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "cpf", nullable = false, unique = true)
    private String cpf;

    private String phone;

    public Passenger(String name, String cpf, String phone) {
        this.name = name;
        this.cpf = normalizeDocument(cpf);
        this.phone = normalizePhone(phone);
    }

    public static String normalizeDocument(String document) {
        if (document == null) {
            return null;
        }
        String normalized = document.replaceAll(DOCUMENT_SEPARATORS, "").toUpperCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    public static String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String digits = phone.replaceAll(NON_DIGITS, "");
        return digits.isEmpty() ? null : digits;
    }
}
