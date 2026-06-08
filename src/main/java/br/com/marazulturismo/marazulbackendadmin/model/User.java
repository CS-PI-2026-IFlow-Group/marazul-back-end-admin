package br.com.marazulturismo.marazulbackendadmin.model;

import br.com.marazulturismo.marazulbackendadmin.enums.Position;
import br.com.marazulturismo.marazulbackendadmin.enums.UserRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.Date;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private Date admissionDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Position position;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole userRole;

    @Column(nullable = true, unique = true)
    private String email;

    @Column(name = "senha_hash")
    private String senhaHash;

    private Date disabledAt;

    public User(
            String nome,
            Date admissionDate,
            Position position,
            UserRole userRole,
            String email,
            String senhaHash) {

        this.nome = nome;
        this.admissionDate = admissionDate;
        this.position = position;
        this.userRole = userRole;
        this.email = email;
        this.senhaHash = senhaHash;
    }
}
