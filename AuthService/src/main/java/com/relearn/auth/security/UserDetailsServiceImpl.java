package com.relearn.auth.security;

import com.relearn.auth.entity.User;
import com.relearn.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Loads user-specific data for Spring Security during authentication.
 *
 * Also enforces the active flag — deactivated users cannot log in.
 * Spring Security's DaoAuthenticationProvider calls this on every login attempt.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with email: " + email));

        SimpleGrantedAuthority authority =
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name());

        /*
         * Spring Security's User constructor accepts:
         *   username, password, enabled, accountNonExpired,
         *   credentialsNonExpired, accountNonLocked, authorities
         *
         * We use user.isActive() for the `enabled` flag.
         * If active=false, Spring Security throws DisabledException on login.
         */
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                user.isActive(),   // enabled — false blocks login
                true,              // accountNonExpired
                true,              // credentialsNonExpired
                true,              // accountNonLocked
                List.of(authority)
        );
    }
}
