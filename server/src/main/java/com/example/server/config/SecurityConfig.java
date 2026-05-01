package com.example.server.config;

import com.example.server.common.util.JwtFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> {
        })
                .csrf(crsf -> crsf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/register", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/client", "/log", "/task/maintenance/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/admin/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/task/**").authenticated()
                        .anyRequest().authenticated()
                )
                // 登录注册接口放行
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)); // 无状态
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
