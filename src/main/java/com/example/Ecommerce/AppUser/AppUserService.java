package com.example.Ecommerce.AppUser;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.Ecommerce.AppUser.DTOs.RegisterRequest;
import com.example.Ecommerce.Common.Exceptions.UserAlreadyExistsException;



@Service
public class AppUserService {

    private PasswordEncoder passwordEncoder;
    private AppUserRepository userRepository;

    public AppUserService(PasswordEncoder passwordEncoder, AppUserRepository userRepository) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    @Transactional
    public void registerUser(RegisterRequest request) {
        if (userRepository.findByUserName(request.getUserName()).isPresent()) {
            throw new UserAlreadyExistsException();
        }

        AppUser user = new AppUser();

        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);
        user.setUserName(request.getUserName());

        userRepository.save(user);
    }

}
