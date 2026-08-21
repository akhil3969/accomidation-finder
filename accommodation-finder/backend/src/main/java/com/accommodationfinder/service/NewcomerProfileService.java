package com.accommodationfinder.service;

import com.accommodationfinder.dto.NewcomerDtos.ArrivalProfile;
import com.accommodationfinder.dto.NewcomerDtos.ArrivalProfileRequest;
import com.accommodationfinder.exception.ResourceNotFoundException;
import com.accommodationfinder.model.User;
import com.accommodationfinder.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Locale;

/**
 * Stores the handful of facts that let the rest of the app give specific rather than
 * generic answers: where you are from, whether you study, whether you hold a scholarship,
 * and where you need to travel to each day.
 *
 * <p>Kept deliberately short. Every extra question is another reason to abandon the form,
 * and each of these earns its place by changing something the user actually sees.
 */
@Service
public class NewcomerProfileService {

    /** EU/EEA plus Switzerland - these nationals need far less paperwork to rent here. */
    private static final List<String> FREE_MOVEMENT = List.of(
            "austria", "belgium", "bulgaria", "croatia", "cyprus", "czechia", "czech republic",
            "denmark", "estonia", "finland", "france", "germany", "greece", "hungary", "iceland",
            "ireland", "italy", "latvia", "liechtenstein", "lithuania", "luxembourg", "malta",
            "netherlands", "norway", "poland", "portugal", "romania", "slovakia", "slovenia",
            "spain", "sweden", "switzerland");

    private final UserRepository userRepository;
    private final GeocodingService geocodingService;

    public NewcomerProfileService(UserRepository userRepository, GeocodingService geocodingService) {
        this.userRepository = userRepository;
        this.geocodingService = geocodingService;
    }

    @Transactional(readOnly = true)
    public ArrivalProfile get(User current) {
        User user = load(current.getId());
        return toProfile(user);
    }

    @Transactional
    public ArrivalProfile save(User current, ArrivalProfileRequest request) {
        User user = load(current.getId());

        if (request.nationality() != null) {
            user.setNationality(request.nationality().trim());
            // Infer free movement, but let an explicit answer override the guess.
            user.setEuNational(request.euNational() != null
                    ? request.euNational()
                    : FREE_MOVEMENT.contains(request.nationality().trim().toLowerCase(Locale.ROOT)));
        } else if (request.euNational() != null) {
            user.setEuNational(request.euNational());
        }

        if (request.dateOfBirth() != null) {
            user.setDateOfBirth(request.dateOfBirth());
        }
        if (request.arrivalDate() != null) {
            user.setArrivalDate(request.arrivalDate());
        }
        if (request.student() != null) {
            user.setStudent(request.student());
        }
        if (request.scholarshipHolder() != null) {
            user.setScholarshipHolder(request.scholarshipHolder());
        }
        if (request.monthlyIncome() != null) {
            user.setMonthlyIncome(request.monthlyIncome());
        }
        if (request.hasFrenchGuarantor() != null) {
            user.setHasFrenchGuarantor(request.hasFrenchGuarantor());
        }

        // Geocode the campus once, so every listing can show a real travel distance.
        if (request.university() != null && !request.university().isBlank()
                && !request.university().equals(user.getUniversity())) {
            user.setUniversity(request.university().trim());
            geocodingService.geocode(request.university().trim(), null, null)
                    .ifPresent(result -> {
                        user.setUniversityLatitude(result.latitude());
                        user.setUniversityLongitude(result.longitude());
                    });
        }

        user.setProfileCompleted(user.getNationality() != null && user.getDateOfBirth() != null);
        return toProfile(userRepository.save(user));
    }

    private User load(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private ArrivalProfile toProfile(User user) {
        Integer age = user.getDateOfBirth() == null
                ? null
                : Period.between(user.getDateOfBirth(), LocalDate.now()).getYears();

        return new ArrivalProfile(
                user.getNationality(),
                user.getEuNational(),
                user.getDateOfBirth(),
                user.getArrivalDate(),
                age,
                user.isStudent(),
                user.isScholarshipHolder(),
                user.getUniversity(),
                user.getUniversityLatitude(),
                user.getUniversityLongitude(),
                user.getMonthlyIncome(),
                user.isHasFrenchGuarantor(),
                user.isProfileCompleted());
    }
}
