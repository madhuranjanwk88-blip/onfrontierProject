package com.hotel.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String ADMIN = "ADMIN";
    private static final String STAFF = "STAFF";

    private final ObjectMapper objectMapper;

    public SecurityConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .headers().frameOptions().sameOrigin().and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
                .authorizeHttpRequests(auth -> auth
                        .antMatchers("/h2-console/**").permitAll()
                        .antMatchers(HttpMethod.GET, "/api/rooms", "/api/rooms/**").permitAll()
                        .antMatchers("/api/rooms/**").hasRole(ADMIN)
                        .antMatchers(HttpMethod.DELETE, "/api/guests/**").hasRole(ADMIN)
                        .antMatchers("/api/guests/**", "/api/bookings/**").hasAnyRole(ADMIN, STAFF)
                        .anyRequest().authenticated())
                .httpBasic().authenticationEntryPoint((req, res, e) ->
                        writeError(req, res, HttpStatus.UNAUTHORIZED, "Authentication required")).and()
                .exceptionHandling()
                .accessDeniedHandler((req, res, e) ->
                        writeError(req, res, HttpStatus.FORBIDDEN, "Access denied"));
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(
            PasswordEncoder encoder,
            @Value("${app.security.admin.username}") String adminUser,
            @Value("${app.security.admin.password}") String adminPass,
            @Value("${app.security.staff.username}") String staffUser,
            @Value("${app.security.staff.password}") String staffPass) {
        return new InMemoryUserDetailsManager(
                User.withUsername(adminUser).password(encoder.encode(adminPass)).roles(ADMIN, STAFF).build(),
                User.withUsername(staffUser).password(encoder.encode(staffPass)).roles(STAFF).build());
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Same body shape as GlobalExceptionHandler, since filter-level errors never reach it
    private void writeError(HttpServletRequest req, HttpServletResponse res, HttpStatus status, String msg)
            throws IOException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("timestamp", LocalDateTime.now().toString());
        m.put("status", status.value());
        m.put("error", msg);
        m.put("path", req.getRequestURI());
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(res.getOutputStream(), m);
    }
}
