package com.luiz.webflux.config;

import com.luiz.webflux.service.UserDetailService;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf().disable()
                .authorizeExchange()
                .pathMatchers(HttpMethod.GET, "/anime/**").hasRole("USER")
                .pathMatchers(HttpMethod.POST, "/anime/**").hasRole("ADMIN")
                .pathMatchers(HttpMethod.PUT, "/anime/**").hasRole("ADMIN")
                .pathMatchers(HttpMethod.DELETE, "/anime/**").hasRole("ADMIN")
                .pathMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/webjars/**").permitAll()
                .anyExchange().authenticated()
                .and().formLogin()
                .and().httpBasic()
                .and().build();
    }

    @Bean
    ReactiveAuthenticationManager authenticationManager(UserDetailService userDetailService) {
        return new UserDetailsRepositoryReactiveAuthenticationManager(userDetailService);
    }

    /**
     * Security in memory
     * @return
     */
//    @Bean
//    public MapReactiveUserDetailsService userDetailsService() {
//        PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
//
//        UserDetails user = User.withUsername("user")
//                .password(passwordEncoder.encode("devdojo"))
//                .roles("USER")
//                .build();
//        UserDetails admin = User.withUsername("admin")
//                .password(passwordEncoder.encode("devdojo"))
//                .roles("USER", "ADMIN")
//                .build();
//
//        return new MapReactiveUserDetailsService(user, admin);
//    }
}
