package products.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
        @Bean
        PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        UserDetailsService userDetailsService(PasswordEncoder encoder) {
                UserDetails admin = User.withUsername("admin").password(encoder.encode("admin123")).roles("ADMIN")
                                .build();
                UserDetails user01 = User.withUsername("user01").password(encoder.encode("123456")).roles("HANHCHINH")
                                .build();
                UserDetails user02 = User.withUsername("user02").password(encoder.encode("123456")).roles("KETOAN")
                                .build();
                return new InMemoryUserDetailsManager(user01, user02, admin);
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) {
                http
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/", "/css/**", "/js/**")
                                                .permitAll()
                                                .requestMatchers("/administration", "/products/list_products",
                                                                "/products/list_products_pageable", "/products/search")
                                                .authenticated()
                                                .requestMatchers("/products/new_product", "/products/save_product")
                                                .hasAnyRole("HANHCHINH", "ADMIN")
                                                .requestMatchers("/**", "/api/product/**")
                                                .hasRole("ADMIN")
                                                .anyRequest().authenticated())
                                .formLogin(form -> form
                                                .loginPage("/login")
                                                .defaultSuccessUrl("/administration", true)
                                                .permitAll())
                                .logout(LogoutConfigurer::permitAll);
                return http.build();
        }

}