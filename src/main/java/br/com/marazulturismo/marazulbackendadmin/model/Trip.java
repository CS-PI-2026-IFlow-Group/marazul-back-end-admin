package br.com.marazulturismo.marazulbackendadmin.model;

import br.com.marazulturismo.marazulbackendadmin.enums.TripStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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

    @Column(columnDefinition = "TEXT")
    private String notes;

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
                client, minimumPassengers, notes, TripStatus.SCHEDULED);
    }

    public void update(City originCity, City destinationCity,
                       LocalDateTime plannedDepartureOrigin, LocalDateTime plannedArrivalDestination,
                       LocalDateTime plannedDepartureDestination, LocalDateTime plannedArrivalOrigin,
                       Vehicle vehicle, Set<Collaborator> drivers, Client client,
                       Integer minimumPassengers, String notes, TripStatus status) {
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
        this.notes = notes;
        if (status != null) {
            this.status = status;
        }
    }

    public List<TripPassenger> getPassengers() {
        return Collections.unmodifiableList(passengers);
    }

    public List<Expense> getExpenses() {
        return Collections.unmodifiableList(expenses);
    }

    public void replacePassengers(List<TripPassenger> newPassengers) {
        this.passengers.clear();
        if (newPassengers != null) {
            for (TripPassenger tp : newPassengers) {
                tp.assignTrip(this);
                this.passengers.add(tp);
            }
        }
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
