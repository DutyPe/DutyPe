package com.parttimes.backend.auth.repositories;

import com.parttimes.backend.auth.models.User;
import com.parttimes.backend.auth.models.UserRole;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByPhoneNumber(String phoneNumber);
    
    boolean existsByEmail(String email);
    
    boolean existsByPhoneNumber(String phoneNumber);
    
    List<User> findByRole(UserRole role);
    
    List<User> findByIsActive(boolean isActive);
    
    List<User> findByIsVerified(boolean isVerified);
    
    List<User> findByLocationContainingIgnoreCase(String location);
    
    List<User> findBySkillsContaining(String skill);
}
