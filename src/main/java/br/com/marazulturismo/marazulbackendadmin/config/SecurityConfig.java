package br.com.marazulturismo.marazulbackendadmin.config;

import br.com.marazulturismo.marazulbackendadmin.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/recuperar-senha",
                                "/api/auth/redefinir-senha"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/dashboard/**")
                        .hasAuthority("dashboard:view")
                        .requestMatchers(HttpMethod.POST, "/api/funcionario/**")
                        .hasAuthority("funcionario:create")
                        .requestMatchers(HttpMethod.GET, "/api/funcionario/**")
                        .hasAuthority("funcionario:view")
                        .requestMatchers(HttpMethod.PUT, "/api/funcionario/**")
                        .hasAuthority("funcionario:edit")
                        .requestMatchers(HttpMethod.DELETE, "/api/funcionario/**")
                        .hasAuthority("funcionario:delete")
                        .requestMatchers(HttpMethod.POST, "/api/frota/**")
                        .hasAuthority("frota:create")
                        .requestMatchers(HttpMethod.GET, "/api/frota/**")
                        .hasAuthority("frota:view")
                        .requestMatchers(HttpMethod.PUT, "/api/frota/**")
                        .hasAuthority("frota:edit")
                        .anyRequest().authenticated())
                .exceptionHandling(ex ->
                        ex.authenticationEntryPoint(unauthorizedEntryPoint()))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(
                    "{\"erro\":\"Autenticação necessária. Credencial ausente ou inválida.\","
                            + "\"message\":\"Autenticação necessária. Credencial ausente ou inválida.\"}");
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
