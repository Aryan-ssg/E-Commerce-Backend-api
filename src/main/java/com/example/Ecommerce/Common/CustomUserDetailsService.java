package com.example.Ecommerce.Common;



import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.Ecommerce.AppUser.AppUser;
import com.example.Ecommerce.AppUser.AppUserRepository;



@Service
public class CustomUserDetailsService implements UserDetailsService {

    private AppUserRepository userRepository;

    public CustomUserDetailsService(AppUserRepository userRepository){
        this.userRepository=userRepository;
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        
        AppUser user=userRepository.findByUsername(username)
        .orElseThrow(() -> new UsernameNotFoundException(username+" not found"));

        return User.withUsername(user.getUsername())
        .password(user.getPassword())
        .authorities("ROLE_"+user.getRole())
        .build();

    }



   


}
