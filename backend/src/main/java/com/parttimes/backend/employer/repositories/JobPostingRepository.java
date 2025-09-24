package com.parttimes.backend.employer.repositories;

import com.parttimes.backend.employer.models.JobPosting;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobPostingRepository extends MongoRepository<JobPosting, String> {
    
    // Find active jobs
    List<JobPosting> findByIsActiveTrue();
    
    // Find jobs by employer
    List<JobPosting> findByEmployerId(String employerId);
    
    // Find jobs by category
    List<JobPosting> findByCategory(String category);
    
    // Find jobs by location
    List<JobPosting> findByCityContainingIgnoreCase(String city);
    List<JobPosting> findByAreaContainingIgnoreCase(String area);
    
    // Find jobs by pay type
    List<JobPosting> findByPayType(String payType);
    
    // Find jobs by job type
    List<JobPosting> findByJobType(String jobType);
    
    // Find verified jobs
    List<JobPosting> findByIsVerifiedTrue();
    
    // Search jobs by title or description
    @Query("{'$or': [{'title': {$regex: ?0, $options: 'i'}}, {'description': {$regex: ?0, $options: 'i'}}]}")
    List<JobPosting> searchJobs(String searchTerm);
    
    // Find jobs with pagination
    Page<JobPosting> findByIsActiveTrue(Pageable pageable);
    
    // Find jobs by multiple criteria
    @Query("{'isActive': true, 'city': {$regex: ?0, $options: 'i'}, 'payType': ?1, 'jobType': ?2}")
    List<JobPosting> findJobsByFilters(String city, String payType, String jobType);
    
    // Find jobs by urgency
    List<JobPosting> findByUrgency(String urgency);
    
    // Find jobs by experience level
    List<JobPosting> findByExperienceRequiredContainingIgnoreCase(String experience);
    
    // Find jobs by gender preference
    List<JobPosting> findByGenderIn(List<String> genders);
    
    // Advanced search methods
    List<JobPosting> findByIsActiveTrueOrderByPostedTimeDesc(Pageable pageable);
    @Query(value = "{'title': {$regex: ?0, $options: 'i'}}", fields = "{'title': 1}")
    List<String> findDistinctTitlesContainingIgnoreCase(String query);
    long countByIsActiveTrue();
    long countByIsVerifiedTrue();
    long countByUrgency(String urgency);
    
    // Paginated search methods
    Page<JobPosting> findByCityContainingIgnoreCase(String city, Pageable pageable);
    Page<JobPosting> findByCategory(String category, Pageable pageable);
    Page<JobPosting> findByPayType(String payType, Pageable pageable);
    Page<JobPosting> findByJobType(String jobType, Pageable pageable);
    Page<JobPosting> findByUrgency(String urgency, Pageable pageable);
    Page<JobPosting> findByIsVerifiedTrue(Pageable pageable);
    @Query("{'$or': [{'title': {$regex: ?0, $options: 'i'}}, {'description': {$regex: ?0, $options: 'i' }}]}")
    Page<JobPosting> searchJobs(String searchTerm, Pageable pageable);
}
