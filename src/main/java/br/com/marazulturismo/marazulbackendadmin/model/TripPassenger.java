package br.com.marazulturismo.marazulbackendadmin.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "trip_passengers", uniqueConstraints = {
        @UniqueConstraint(name = "uk_trip_passenger", columnNames = {"trip_id", "passenger_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TripPassenger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "passenger_id", nullable = false)
    private Passenger passenger;

    @Column(name = "amount_due", precision = 10, scale = 2)
    private BigDecimal amountDue;

    @Column(name = "amount_paid", precision = 10, scale = 2, nullable = false)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    public TripPassenger(Trip trip, Passenger passenger, BigDecimal amountDue, BigDecimal amountPaid) {
        this.trip = trip;
        this.passenger = passenger;
        this.amountDue = amountDue;
        this.amountPaid = amountPaid != null ? amountPaid : BigDecimal.ZERO;
    }

    public TripPassenger(Trip trip, Passenger passenger, BigDecimal amountDue) {
        this(trip, passenger, amountDue, BigDecimal.ZERO);
    }

    public void updateAmounts(BigDecimal amountDue, BigDecimal amountPaid) {
        this.amountDue = amountDue;
        if (amountPaid != null) {
            this.amountPaid = amountPaid;
        }
    }

    void assignTrip(Trip trip) {
        this.trip = trip;
    }
}
