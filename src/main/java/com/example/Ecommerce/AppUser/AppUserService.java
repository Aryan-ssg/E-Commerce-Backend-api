package com.example.Ecommerce.AppUser;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.Ecommerce.AppUser.DTOs.RegisterRequest;

@Service
public class AppUserService {


    private PasswordEncoder passwordEncoder;
    private AppUserRepository userRepository;
    

    
    public AppUserService(PasswordEncoder passwordEncoder, AppUserRepository userRepository) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }


    


    public void registerUser(RegisterRequest request){

        AppUser user = new AppUser();

        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);
        user.setUserName(request.getUserName());

        userRepository.save(user);
    }


  


    
}
