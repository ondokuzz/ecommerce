package com.demirsoft.ecommerce.auth_service.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.demirsoft.ecommerce.auth_service.entity.User;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserService userService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("load by user name called for: " + username);
        User user = userService.findByUsername(username);
        log.info("load by user name returned from findByUsername: " + username);
        if (user == null) {
            log.info("load by user name not found for: " + username);
            throw new UsernameNotFoundException("User not found with username: " + username);
        }
        log.info("load by user name being returned with: " + user.getPassword());
        log.info("load by user name being returned with: " + user.getRole().name());
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority(user.getRole().name())));
    }
}
