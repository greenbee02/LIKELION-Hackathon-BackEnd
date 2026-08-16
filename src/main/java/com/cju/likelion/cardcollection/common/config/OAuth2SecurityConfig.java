package com.cju.likelion.cardcollection.common.config;

import com.cju.likelion.cardcollection.auth.security.OAuth2LoginFailureHandler;
import com.cju.likelion.cardcollection.auth.security.OAuth2LoginSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("oauth")
public class OAuth2SecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain oauth2SecurityFilterChain(
            HttpSecurity http,
            OAuth2LoginSuccessHandler successHandler,
            OAuth2LoginFailureHandler failureHandler
    ) throws Exception {
        http
                .securityMatcher("/oauth2/**", "/login/**")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(successHandler)
                        .failureHandler(failureHandler));

        return http.build();
    }
}
