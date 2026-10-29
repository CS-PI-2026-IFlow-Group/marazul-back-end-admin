package br.com.marazulturismo.marazulbackendadmin.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "expenses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String description;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    public Expense(Trip trip, String description, BigDecimal amount) {
        this.trip = trip;
        this.description = description;
        this.amount = amount;
    }

    public Expense(String description, BigDecimal amount) {
        this(null, description, amount);
    }

    public void update(String description, BigDecimal amount) {
        this.description = description;
        this.amount = amount;
    }

    void assignTrip(Trip trip) {
        this.trip = trip;
    }
}
