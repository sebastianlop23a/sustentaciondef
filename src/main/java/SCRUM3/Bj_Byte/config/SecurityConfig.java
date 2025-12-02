package SCRUM3.Bj_Byte.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // Bean para encriptar y comparar contraseñas
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // desactiva CSRF para desarrollo
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/empleado/**", "/css/**", "/js/**").permitAll() // Ojo: en tu controlador usas /empleado
                .anyRequest().permitAll()
            )
            .formLogin(form -> form.disable()); // desactiva login por defecto

        return http.build();
    }
}
