package com.genrag.config;

// import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
// import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.genrag.auth.internal.JwtAuthFilter;
// import org.springframework.web.cors.CorsConfiguration;
// import org.springframework.web.cors.CorsConfigurationSource;
// import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class Security {
    // private final String frontendUrl;
    private final JwtAuthFilter jwtAuthFilter;

    public Security(JwtAuthFilter jwtAuthFilter) {
        // this.frontendUrl = frontendUrl;
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth ->
                auth
                    .requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers("/api/internal/**").permitAll()
                    .anyRequest().authenticated()
            )
            .exceptionHandling(ex ->
                ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
            // .cors(Customizer.withDefaults())     // Proxy approch won't require cors due to server to server communication

        return http.build();
    }

    // Proxy approch won't require cors due to server to server communication
    // @Bean
    // public CorsConfigurationSource corsConfigurationSource() {
    //     String[] origins = new String[] { frontendUrl };
    //     String[] headers = new String[] { "Authorization", "Content-Type" };
    //     String[] methods = new String[] { "GET", "POST", "OPTIONS" };   // More will be added like PUT/DELETE in future releases
        
    //     CorsConfiguration corsConfig = new CorsConfiguration();
    //     corsConfig.setAllowedOrigins(List.of(origins));
    //     corsConfig.setAllowedHeaders(List.of(headers));
    //     corsConfig.setAllowedMethods(List.of(methods));

    //     corsConfig.setAllowCredentials(true);

    //     UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    //     source.registerCorsConfiguration("/**", corsConfig);

    //     return source;
    // }
}
