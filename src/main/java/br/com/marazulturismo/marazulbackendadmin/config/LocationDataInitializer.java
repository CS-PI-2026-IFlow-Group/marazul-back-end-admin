package br.com.marazulturismo.marazulbackendadmin.config;

import br.com.marazulturismo.marazulbackendadmin.model.City;
import br.com.marazulturismo.marazulbackendadmin.model.State;
import br.com.marazulturismo.marazulbackendadmin.repository.CityRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.StateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Carrega estados e cidades a partir dos arquivos versionados em {@code src/main/resources/data},
 * gerados com os dados de localidades do IBGE.
 */
@Component
public class LocationDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(LocationDataInitializer.class);

    static final String STATES_FILE = "data/states.json";
    static final String CITIES_FILE = "data/cities.json";

    private final StateRepository stateRepository;
    private final CityRepository cityRepository;
    private final ObjectMapper objectMapper;

    public LocationDataInitializer(
            StateRepository stateRepository,
            CityRepository cityRepository,
            ObjectMapper objectMapper) {
        this.stateRepository = stateRepository;
        this.cityRepository = cityRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void run(@NonNull String... args) {
        seedStates();
        seedCities();
    }

    // Cada tabela é carregada só quando está vazia, então reinicializar não duplica nada.
    // A verificação é por tabela, e não uma única no começo, para que as cidades sejam importadas
    // mesmo numa base que já tenha os estados.
    private void seedStates() {
        if (stateRepository.count() > 0) {
            return;
        }

        List<State> states = read(STATES_FILE, new TypeReference<List<StateSeed>>() {}).stream()
                .map(seed -> new State(seed.name(), seed.acronym()))
                .toList();

        stateRepository.saveAll(states);
        log.info(" [SEED] {} estados importados de {}.", states.size(), STATES_FILE);
    }

    private void seedCities() {
        if (cityRepository.count() > 0) {
            return;
        }

        Map<String, State> statesByAcronym = stateRepository.findAll().stream()
                .collect(Collectors.toMap(State::getAcronym, Function.identity()));

        List<City> cities = read(CITIES_FILE, new TypeReference<List<CitySeed>>() {}).stream()
                .map(seed -> new City(seed.name(), stateOf(seed, statesByAcronym)))
                .toList();

        cityRepository.saveAll(cities);
        log.info(" [SEED] {} cidades importadas de {}.", cities.size(), CITIES_FILE);
    }

    private State stateOf(CitySeed seed, Map<String, State> statesByAcronym) {
        State state = statesByAcronym.get(seed.state());
        if (state == null) {
            throw new IllegalStateException(
                    "A cidade %s em %s referencia o estado %s, que não existe em %s."
                            .formatted(seed.name(), CITIES_FILE, seed.state(), STATES_FILE));
        }
        return state;
    }

    private <T> T read(String file, TypeReference<T> type) {
        try (InputStream data = new ClassPathResource(file).getInputStream()) {
            return objectMapper.readValue(data, type);
        } catch (IOException | JacksonException e) {
            throw new IllegalStateException("Falha ao ler os dados de carga de " + file + ".", e);
        }
    }

    record StateSeed(String name, String acronym) {
    }

    record CitySeed(String name, String state) {
    }
}
