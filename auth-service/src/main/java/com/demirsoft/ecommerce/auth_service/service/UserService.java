package com.demirsoft.ecommerce.auth_service.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.demirsoft.ecommerce.auth_service.entity.Role;
import com.demirsoft.ecommerce.auth_service.entity.User;
import com.demirsoft.ecommerce.auth_service.exception.UserOperationNotPermittedException;
import com.demirsoft.ecommerce.auth_service.exception.UserNotFoundException;
import com.demirsoft.ecommerce.auth_service.repository.UserRepository;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User save(User user) {
        log.debug("saving user with passwd1: " + user.getPassword());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        log.debug("saving user with passwd2: " + user.getPassword());
        return userRepository.save(user);
    }

    public User update(User user) {
        log.debug("update user : " + user.getUsername());
        var dbUser = userRepository.findByUsername(user.getUsername());

        if (dbUser == null)
            throw new UserNotFoundException(user.getUsername());

        if (user.getRole() != dbUser.getRole() && dbUser.getRole() != Role.ADMIN)
            throw new UserOperationNotPermittedException(
                    String.format("user: %s with role: %s can not modify its own role",
                            user.getUsername(),
                            user.getRole().name()));

        dbUser.setPassword(passwordEncoder.encode(user.getPassword()));
        dbUser.setRole(user.getRole());
        dbUser.setEmail(user.getEmail());
        return dbUser;
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}
