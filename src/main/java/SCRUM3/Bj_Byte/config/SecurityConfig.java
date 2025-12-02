package SCRUM3.Bj_Byte.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable()) // OK para pruebas
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/empleado/**",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/public/**",
                        "/error",
                        "/"
                ).permitAll()
                .anyRequest().authenticated() // Spring exige esto aunque luego desactives el login
            )
            .formLogin(form -> form.disable()) // Puedes desactivar el login
            .httpBasic(httpBasic -> httpBasic.disable()); // También el basic auth

        return http.build();
    }
}
