package com.james.taskflow.auth;

import com.james.taskflow.auth.dto.RegisterRequest;
import com.james.taskflow.common.exception.ConflictException;
import com.james.taskflow.user.User;
import com.james.taskflow.user.UserRepository;
import com.james.taskflow.user.dto.UserResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse register(RegisterRequest req) {
        String email = normalize(req.email());

        if(users.existsByEmail(email)) {
            throw new ConflictException("Email is already registered!");
        }

        User user = new User(email, passwordEncoder.encode(req.password()));

        try{
            return UserResponse.from(users.saveAndFlush(user));
        }catch(DataIntegrityViolationException ex){
            throw new ConflictException("Email is already registered!");
        }
    }

    private static String normalize(String email){
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
