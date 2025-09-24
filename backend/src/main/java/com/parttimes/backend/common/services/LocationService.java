package com.parttimes.backend.common.services;

import com.parttimes.backend.employer.models.JobPosting;
import com.parttimes.backend.employer.repositories.JobPostingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class LocationService {

    @Autowired
    private JobPostingRepository jobPostingRepository;

    // Major Indian cities with coordinates
    private static final Map<String, double[]> MAJOR_CITIES = new HashMap<String, double[]>() {{
        put("Mumbai", new double[]{19.0760, 72.8777});
        put("Delhi", new double[]{28.7041, 77.1025});
        put("Bangalore", new double[]{12.9716, 77.5946});
        put("Hyderabad", new double[]{17.3850, 78.4867});
        put("Chennai", new double[]{13.0827, 80.2707});
        put("Kolkata", new double[]{22.5726, 88.3639});
        put("Pune", new double[]{18.5204, 73.8567});
        put("Ahmedabad", new double[]{23.0225, 72.5714});
        put("Jaipur", new double[]{26.9124, 75.7873});
        put("Surat", new double[]{21.1702, 72.8311});
        put("Lucknow", new double[]{26.8467, 80.9462});
        put("Kanpur", new double[]{26.4499, 80.3319});
        put("Nagpur", new double[]{21.1458, 79.0882});
        put("Indore", new double[]{22.7196, 75.8577});
        put("Thane", new double[]{19.2183, 72.9781});
        put("Bhopal", new double[]{23.2599, 77.4126});
        put("Visakhapatnam", new double[]{17.6868, 83.2185});
        put("Pimpri-Chinchwad", new double[]{18.6298, 73.7997});
        put("Patna", new double[]{25.5941, 85.1376});
        put("Vadodara", new double[]{22.3072, 73.1812});
    }};

    public List<JobPosting> getJobsNearLocation(String city, double radiusKm) {
        List<JobPosting> allJobs = jobPostingRepository.findByIsActiveTrue();
        
        if (!MAJOR_CITIES.containsKey(city)) {
            // If city not in major cities, return jobs with exact city match
            return allJobs.stream()
                .filter(job -> job.getCity() != null && job.getCity().equalsIgnoreCase(city))
                .collect(Collectors.toList());
        }

        double[] cityCoords = MAJOR_CITIES.get(city);
        return allJobs.stream()
            .filter(job -> isWithinRadius(job, cityCoords, radiusKm))
            .collect(Collectors.toList());
    }

    public List<JobPosting> getJobsByDistance(String userCity, double userLat, double userLon, double maxDistanceKm) {
        List<JobPosting> allJobs = jobPostingRepository.findByIsActiveTrue();
        
        return allJobs.stream()
            .filter(job -> {
                double distance = calculateDistance(userLat, userLon, job);
                return distance <= maxDistanceKm;
            })
            .sorted((job1, job2) -> {
                double dist1 = calculateDistance(userLat, userLon, job1);
                double dist2 = calculateDistance(userLat, userLon, job2);
                return Double.compare(dist1, dist2);
            })
            .collect(Collectors.toList());
    }

    public Map<String, Object> getLocationBasedJobStats(String city) {
        List<JobPosting> cityJobs = getJobsNearLocation(city, 50); // 50km radius
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalJobs", cityJobs.size());
        stats.put("city", city);
        
        // Job categories in the city
        Map<String, Long> categoryCount = cityJobs.stream()
            .collect(Collectors.groupingBy(
                job -> job.getCategory() != null ? job.getCategory() : "Other",
                Collectors.counting()
            ));
        stats.put("categories", categoryCount);
        
        // Average pay by category
        Map<String, Double> avgPayByCategory = cityJobs.stream()
            .filter(job -> job.getPayAmount() != null && !job.getPayAmount().isEmpty())
            .collect(Collectors.groupingBy(
                job -> job.getCategory() != null ? job.getCategory() : "Other",
                Collectors.averagingDouble(job -> {
                    try {
                        return Double.parseDouble(job.getPayAmount().replaceAll("[^0-9.]", ""));
                    } catch (NumberFormatException e) {
                        return 0.0;
                    }
                })
            ));
        stats.put("averagePayByCategory", avgPayByCategory);
        
        // Job types distribution
        Map<String, Long> jobTypeCount = cityJobs.stream()
            .collect(Collectors.groupingBy(
                job -> job.getJobType() != null ? job.getJobType() : "Other",
                Collectors.counting()
            ));
        stats.put("jobTypes", jobTypeCount);
        
        return stats;
    }

    public List<String> getNearbyCities(String city, double radiusKm) {
        if (!MAJOR_CITIES.containsKey(city)) {
            return Collections.emptyList();
        }

        double[] cityCoords = MAJOR_CITIES.get(city);
        
        return MAJOR_CITIES.entrySet().stream()
            .filter(entry -> !entry.getKey().equals(city))
            .filter(entry -> {
                double distance = calculateDistance(
                    cityCoords[0], cityCoords[1],
                    entry.getValue()[0], entry.getValue()[1]
                );
                return distance <= radiusKm;
            })
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    public List<JobPosting> getJobsInMultipleCities(List<String> cities, double radiusKm) {
        Set<JobPosting> allJobs = new HashSet<>();
        
        for (String city : cities) {
            List<JobPosting> cityJobs = getJobsNearLocation(city, radiusKm);
            allJobs.addAll(cityJobs);
        }
        
        return new ArrayList<>(allJobs);
    }

    public Map<String, Double> calculateDistancesToJobs(String userCity, List<JobPosting> jobs) {
        Map<String, Double> distances = new HashMap<>();
        
        if (!MAJOR_CITIES.containsKey(userCity)) {
            return distances;
        }
        
        double[] userCoords = MAJOR_CITIES.get(userCity);
        
        for (JobPosting job : jobs) {
            double distance = calculateDistance(userCoords[0], userCoords[1], job);
            distances.put(job.getJobId(), distance);
        }
        
        return distances;
    }

    private boolean isWithinRadius(JobPosting job, double[] centerCoords, double radiusKm) {
        if (job.getCity() == null) {
            return false;
        }
        
        // If job city is in major cities, calculate distance
        if (MAJOR_CITIES.containsKey(job.getCity())) {
            double[] jobCoords = MAJOR_CITIES.get(job.getCity());
            double distance = calculateDistance(
                centerCoords[0], centerCoords[1],
                jobCoords[0], jobCoords[1]
            );
            return distance <= radiusKm;
        }
        
        // If job city matches center city, include it
        return job.getCity().equalsIgnoreCase(getCityName(centerCoords));
    }

    private double calculateDistance(double lat1, double lon1, JobPosting job) {
        if (job.getCity() == null || !MAJOR_CITIES.containsKey(job.getCity())) {
            return Double.MAX_VALUE; // Very far if city not found
        }
        
        double[] jobCoords = MAJOR_CITIES.get(job.getCity());
        return calculateDistance(lat1, lon1, jobCoords[0], jobCoords[1]);
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth in km
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c; // Distance in km
    }

    private String getCityName(double[] coords) {
        return MAJOR_CITIES.entrySet().stream()
            .filter(entry -> Arrays.equals(entry.getValue(), coords))
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse("Unknown");
    }

    public List<String> getPopularCities() {
        List<JobPosting> allJobs = jobPostingRepository.findByIsActiveTrue();
        
        return allJobs.stream()
            .filter(job -> job.getCity() != null)
            .collect(Collectors.groupingBy(
                JobPosting::getCity,
                Collectors.counting()
            ))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(20)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    public Map<String, Object> getLocationInsights() {
        List<JobPosting> allJobs = jobPostingRepository.findByIsActiveTrue();
        
        Map<String, Object> insights = new HashMap<>();
        
        // Top cities by job count
        Map<String, Long> topCities = allJobs.stream()
            .filter(job -> job.getCity() != null)
            .collect(Collectors.groupingBy(
                JobPosting::getCity,
                Collectors.counting()
            ))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(10)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));
        insights.put("topCities", topCities);
        
        // Average pay by city
        Map<String, Double> avgPayByCity = allJobs.stream()
            .filter(job -> job.getCity() != null && job.getPayAmount() != null && !job.getPayAmount().isEmpty())
            .collect(Collectors.groupingBy(
                JobPosting::getCity,
                Collectors.averagingDouble(job -> {
                    try {
                        return Double.parseDouble(job.getPayAmount().replaceAll("[^0-9.]", ""));
                    } catch (NumberFormatException e) {
                        return 0.0;
                    }
                })
            ));
        insights.put("averagePayByCity", avgPayByCity);
        
        return insights;
    }
}
