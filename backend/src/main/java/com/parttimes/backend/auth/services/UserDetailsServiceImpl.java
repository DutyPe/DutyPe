package com.parttimes.backend.auth.services;

import com.parttimes.backend.auth.models.User;
import com.parttimes.backend.auth.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // For phone-based auth, username is actually the user ID
        User user = userRepository.findById(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + username));
        
        return user;
    }
}
