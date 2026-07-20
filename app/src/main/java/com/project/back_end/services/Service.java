package com.project.back_end.services;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Admin;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@org.springframework.stereotype.Service
public class Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(Service.class);
    private static final DateTimeFormatter TWELVE_HOUR_TIME =
            DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    private final TokenService tokenService;
    private final AdminRepository adminRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final DoctorService doctorService;
    private final PatientService patientService;

    public Service(
            TokenService tokenService,
            AdminRepository adminRepository,
            DoctorRepository doctorRepository,
            PatientRepository patientRepository,
            DoctorService doctorService,
            PatientService patientService) {
        this.tokenService = tokenService;
        this.adminRepository = adminRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.doctorService = doctorService;
        this.patientService = patientService;
    }

    public ResponseEntity<Map<String, String>> validateToken(String token, String user) {
        try {
            if (tokenService.validateToken(token, user)) {
                return ResponseEntity.ok(Collections.emptyMap());
            }
        } catch (Exception exception) {
            LOGGER.warn("Token validation failed for role {}", user, exception);
        }

        return response(HttpStatus.UNAUTHORIZED, "Invalid or expired token.");
    }

    public ResponseEntity<Map<String, String>> validateAdmin(Admin receivedAdmin) {
        if (receivedAdmin == null
                || receivedAdmin.getUsername() == null
                || receivedAdmin.getPassword() == null) {
            return response(HttpStatus.BAD_REQUEST, "Username and password are required.");
        }

        try {
            Admin storedAdmin = adminRepository.findByUsername(receivedAdmin.getUsername());
            if (storedAdmin == null
                    || !Objects.equals(storedAdmin.getPassword(), receivedAdmin.getPassword())) {
                return response(HttpStatus.UNAUTHORIZED, "Invalid credentials.");
            }

            String token = tokenService.generateToken(storedAdmin.getUsername());
            return ResponseEntity.ok(Map.of("token", token));
        } catch (Exception exception) {
            LOGGER.error("Unable to validate admin credentials", exception);
            return response(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to validate admin credentials.");
        }
    }

    public Map<String, Object> filterDoctor(String name, String specialty, String time) {
        boolean hasName = hasFilterValue(name);
        boolean hasSpecialty = hasFilterValue(specialty);
        boolean hasTime = hasFilterValue(time);

        if (hasName && hasSpecialty && hasTime) {
            return doctorService.filterDoctorsByNameSpecilityandTime(name, specialty, time);
        }
        if (hasName && hasTime) {
            return doctorService.filterDoctorByNameAndTime(name, time);
        }
        if (hasName && hasSpecialty) {
            return doctorService.filterDoctorByNameAndSpecility(name, specialty);
        }
        if (hasTime && hasSpecialty) {
            return doctorService.filterDoctorByTimeAndSpecility(specialty, time);
        }
        if (hasName) {
            return doctorService.findDoctorByName(name);
        }
        if (hasSpecialty) {
            return doctorService.filterDoctorBySpecility(specialty);
        }
        if (hasTime) {
            return doctorService.filterDoctorsByTime(time);
        }

        return Map.of("doctors", doctorService.getDoctors());
    }

    public int validateAppointment(Appointment appointment) {
        if (appointment == null
                || appointment.getDoctor() == null
                || appointment.getDoctor().getId() == null
                || appointment.getAppointmentTime() == null) {
            return 0;
        }

        Optional<Doctor> doctor = doctorRepository.findById(appointment.getDoctor().getId());
        if (doctor.isEmpty()) {
            return -1;
        }

        try {
            List<String> availableSlots = doctorService.getDoctorAvailability(
                    doctor.get().getId(), appointment.getAppointmentTime().toLocalDate());
            LocalTime requestedTime = appointment.getAppointmentTime().toLocalTime();

            return availableSlots.stream()
                    .map(this::parseSlotStart)
                    .flatMap(Optional::stream)
                    .anyMatch(requestedTime::equals) ? 1 : 0;
        } catch (Exception exception) {
            LOGGER.error("Unable to validate appointment availability", exception);
            return 0;
        }
    }

    public boolean validatePatient(Patient patient) {
        if (patient == null) {
            return false;
        }

        return patientRepository.findByEmailOrPhone(patient.getEmail(), patient.getPhone()) == null;
    }

    public ResponseEntity<Map<String, String>> validatePatientLogin(Login login) {
        if (login == null || login.getIdentifier() == null || login.getPassword() == null) {
            return response(HttpStatus.BAD_REQUEST, "Email and password are required.");
        }

        try {
            Patient patient = patientRepository.findByEmail(login.getIdentifier());
            if (patient == null || !Objects.equals(patient.getPassword(), login.getPassword())) {
                return response(HttpStatus.UNAUTHORIZED, "Invalid credentials.");
            }

            String token = tokenService.generateToken(patient.getEmail());
            return ResponseEntity.ok(Map.of("token", token));
        } catch (Exception exception) {
            LOGGER.error("Unable to validate patient credentials", exception);
            return response(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to validate patient credentials.");
        }
    }

    public ResponseEntity<Map<String, Object>> filterPatient(
            String condition, String name, String token) {
        try {
            String email = tokenService.extractIdentifier(token);
            Patient patient = patientRepository.findByEmail(email);
            if (patient == null) {
                return objectResponse(HttpStatus.UNAUTHORIZED, "Invalid patient token.");
            }

            boolean hasCondition = hasFilterValue(condition);
            boolean hasName = hasFilterValue(name);

            if (hasCondition && hasName) {
                return patientService.filterByDoctorAndCondition(
                        condition, name, patient.getId());
            }
            if (hasCondition) {
                return patientService.filterByCondition(condition, patient.getId());
            }
            if (hasName) {
                return patientService.filterByDoctor(name, patient.getId());
            }

            return patientService.getPatientAppointment(patient.getId(), token);
        } catch (Exception exception) {
            LOGGER.warn("Unable to filter patient appointments", exception);
            return objectResponse(HttpStatus.UNAUTHORIZED, "Invalid or expired token.");
        }
    }

    private Optional<LocalTime> parseSlotStart(String slot) {
        if (slot == null || slot.isBlank()) {
            return Optional.empty();
        }

        String start = slot.split("-", 2)[0].trim().toUpperCase(Locale.ROOT);
        try {
            return Optional.of(LocalTime.parse(start, DateTimeFormatter.ofPattern("H:mm")));
        } catch (DateTimeParseException ignored) {
            try {
                return Optional.of(LocalTime.parse(start, TWELVE_HOUR_TIME));
            } catch (DateTimeParseException invalidTime) {
                return Optional.empty();
            }
        }
    }

    private boolean hasFilterValue(String value) {
        return value != null
                && !value.isBlank()
                && !"null".equalsIgnoreCase(value.trim());
    }

    private ResponseEntity<Map<String, String>> response(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("message", message));
    }

    private ResponseEntity<Map<String, Object>> objectResponse(
            HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("message", message));
    }
}
