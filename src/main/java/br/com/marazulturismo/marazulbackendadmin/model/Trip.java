package br.com.marazulturismo.marazulbackendadmin.model;

import br.com.marazulturismo.marazulbackendadmin.enums.TripStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Entity
@Table(name = "trips")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "origin_city_id", nullable = false)
    private City originCity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_city_id", nullable = false)
    private City destinationCity;

    @Column(name = "planned_departure_origin", nullable = false)
    private LocalDateTime plannedDepartureOrigin;

    @Column(name = "planned_arrival_destination", nullable = false)
    private LocalDateTime plannedArrivalDestination;

    @Column(name = "planned_departure_destination", nullable = false)
    private LocalDateTime plannedDepartureDestination;

    @Column(name = "planned_arrival_origin", nullable = false)
    private LocalDateTime plannedArrivalOrigin;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "trip_drivers",
            joinColumns = @JoinColumn(name = "trip_id", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "collaborator_id", nullable = false))
    private Set<Collaborator> drivers = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TripStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    private Integer minimumPassengers;

    @Column(name = "estimated_passenger_amount", precision = 10, scale = 2)
    private BigDecimal estimatedPassengerAmount;

    @Column(name = "estimated_total_value", precision = 10, scale = 2)
    private BigDecimal estimatedTotalValue;

    @Column(name = "estimated_individual_value", precision = 10, scale = 2)
    private BigDecimal estimatedIndividualValue;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_passenger_id")
    private Passenger responsiblePassenger;

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TripPassenger> passengers = new ArrayList<>();

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Expense> expenses = new ArrayList<>();

    public Trip(City originCity, City destinationCity,
                LocalDateTime plannedDepartureOrigin, LocalDateTime plannedArrivalDestination,
                LocalDateTime plannedDepartureDestination, LocalDateTime plannedArrivalOrigin,
                Vehicle vehicle, Set<Collaborator> drivers) {
        this(originCity, destinationCity, plannedDepartureOrigin, plannedArrivalDestination,
                plannedDepartureDestination, plannedArrivalOrigin, vehicle, drivers, null, null, null);
    }

    public Trip(City originCity, City destinationCity,
                LocalDateTime plannedDepartureOrigin, LocalDateTime plannedArrivalDestination,
                LocalDateTime plannedDepartureDestination, LocalDateTime plannedArrivalOrigin,
                Vehicle vehicle, Set<Collaborator> drivers, Client client,
                Integer minimumPassengers, String notes) {
        update(originCity, destinationCity, plannedDepartureOrigin, plannedArrivalDestination,
                plannedDepartureDestination, plannedArrivalOrigin, vehicle, drivers,
                client, minimumPassengers, null, notes, null, TripStatus.SCHEDULED);
    }

    public void update(City originCity, City destinationCity,
                       LocalDateTime plannedDepartureOrigin, LocalDateTime plannedArrivalDestination,
                       LocalDateTime plannedDepartureDestination, LocalDateTime plannedArrivalOrigin,
                       Vehicle vehicle, Set<Collaborator> drivers, Client client,
                       Integer minimumPassengers, String notes, TripStatus status) {
        update(originCity, destinationCity, plannedDepartureOrigin, plannedArrivalDestination,
                plannedDepartureDestination, plannedArrivalOrigin, vehicle, drivers, client,
                minimumPassengers, this.estimatedPassengerAmount, this.estimatedTotalValue,
                this.estimatedIndividualValue, notes, this.responsiblePassenger, status);
    }

    public void update(City originCity, City destinationCity,
                       LocalDateTime plannedDepartureOrigin, LocalDateTime plannedArrivalDestination,
                       LocalDateTime plannedDepartureDestination, LocalDateTime plannedArrivalOrigin,
                       Vehicle vehicle, Set<Collaborator> drivers, Client client,
                       Integer minimumPassengers, BigDecimal estimatedPassengerAmount,
                       String notes, Passenger responsiblePassenger, TripStatus status) {
        update(originCity, destinationCity, plannedDepartureOrigin, plannedArrivalDestination,
                plannedDepartureDestination, plannedArrivalOrigin, vehicle, drivers, client,
                minimumPassengers, estimatedPassengerAmount, this.estimatedTotalValue,
                this.estimatedIndividualValue, notes, responsiblePassenger, status);
    }

    public void update(City originCity, City destinationCity,
                       LocalDateTime plannedDepartureOrigin, LocalDateTime plannedArrivalDestination,
                       LocalDateTime plannedDepartureDestination, LocalDateTime plannedArrivalOrigin,
                       Vehicle vehicle, Set<Collaborator> drivers, Client client,
                       Integer minimumPassengers, BigDecimal estimatedPassengerAmount,
                       BigDecimal estimatedTotalValue, BigDecimal estimatedIndividualValue,
                       String notes, Passenger responsiblePassenger, TripStatus status) {
        this.originCity = originCity;
        this.destinationCity = destinationCity;
        this.plannedDepartureOrigin = plannedDepartureOrigin;
        this.plannedArrivalDestination = plannedArrivalDestination;
        this.plannedDepartureDestination = plannedDepartureDestination;
        this.plannedArrivalOrigin = plannedArrivalOrigin;
        this.vehicle = vehicle;
        this.drivers = drivers == null ? new HashSet<>() : new HashSet<>(drivers);
        this.client = client;
        this.minimumPassengers = minimumPassengers;
        this.estimatedPassengerAmount = estimatedPassengerAmount;
        this.estimatedTotalValue = estimatedTotalValue;
        this.estimatedIndividualValue = estimatedIndividualValue;
        this.notes = notes;
        this.responsiblePassenger = responsiblePassenger;
        if (status != null) {
            this.status = status;
        }
    }

    public void setResponsiblePassenger(Passenger responsiblePassenger) {
        this.responsiblePassenger = responsiblePassenger;
    }

    public void confirm() {
        this.status = TripStatus.CONFIRMED;
    }

    public List<TripPassenger> getPassengers() {
        return Collections.unmodifiableList(passengers);
    }

    public List<Expense> getExpenses() {
        return Collections.unmodifiableList(expenses);
    }

    public void replacePassengers(List<TripPassenger> newPassengers) {
        Map<Long, TripPassenger> currentByPassengerId = new HashMap<>();
        for (TripPassenger current : this.passengers) {
            currentByPassengerId.put(current.getPassenger().getId(), current);
        }

        List<TripPassenger> kept = new ArrayList<>();
        List<TripPassenger> added = new ArrayList<>();
        if (newPassengers != null) {
            for (TripPassenger tp : newPassengers) {
                TripPassenger existing = currentByPassengerId.get(tp.getPassenger().getId());
                if (existing != null) {
                    existing.updateAmounts(tp.getAmountDue(), tp.getAmountPaid());
                    kept.add(existing);
                } else {
                    tp.assignTrip(this);
                    added.add(tp);
                }
            }
        }

        this.passengers.removeIf(current -> !kept.contains(current));
        this.passengers.addAll(added);
    }

    public void addPassenger(TripPassenger tripPassenger) {
        if (tripPassenger != null) {
            tripPassenger.assignTrip(this);
            this.passengers.add(tripPassenger);
        }
    }

    public void replaceExpenses(List<Expense> newExpenses) {
        this.expenses.clear();
        if (newExpenses != null) {
            for (Expense expense : newExpenses) {
                expense.assignTrip(this);
                this.expenses.add(expense);
            }
        }
    }

    public void addExpense(Expense expense) {
        if (expense != null) {
            expense.assignTrip(this);
            this.expenses.add(expense);
        }
    }
}
