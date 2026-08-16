package com.cju.likelion.cardcollection.auth.security;

import com.cju.likelion.cardcollection.auth.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, UserRepository userRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = resolveToken(request);

        if (token != null
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                if (jwtTokenProvider.isValid(token)) {
                    var authentication = jwtTokenProvider.getAuthentication(token);
                    var userId = java.util.UUID.fromString(authentication.getName());
                    if (userRepository.findByIdAndDeletedAtIsNull(userId).isPresent()) {
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            } catch (IllegalArgumentException exception) {
                // 서명은 유효하지만 role 등 클레임 값이 잘못된 토큰은 인증하지 않는다.
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return null;
    }
}
