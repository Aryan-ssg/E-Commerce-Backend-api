package com.example.Ecommerce.Common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.Ecommerce.AppUser.AppUser;
import com.example.Ecommerce.AppUser.AppUserRepository;
import com.example.Ecommerce.AppUser.Role;



@Component
public class DataInitializer implements CommandLineRunner {

    private AppUserRepository userRepo;
    private PasswordEncoder passwordEncoder;
    @Value("${app.admin.password}")
    private String adminPassword;

    public DataInitializer(AppUserRepository userRepo,PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder=passwordEncoder;
     
    }

    @Override
    public void run(String... args) {
        if (userRepo.findByUserName("admin").isEmpty()) {
            AppUser admin=new AppUser();
            admin.setUserName("admin");
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole(Role.ADMIN);

            userRepo.save(admin);

        }

    }

}
