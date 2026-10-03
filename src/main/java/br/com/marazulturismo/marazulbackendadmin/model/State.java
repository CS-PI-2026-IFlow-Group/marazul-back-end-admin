package br.com.marazulturismo.marazulbackendadmin.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "state")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class State {

    public static final int ACRONYM_LENGTH = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, length = ACRONYM_LENGTH)
    private String acronym;

    public State(String name, String acronym) {
        this.name = name;
        this.acronym = acronym;
    }
}
