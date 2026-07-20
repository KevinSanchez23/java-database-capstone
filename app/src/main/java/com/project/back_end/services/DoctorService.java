package com.project.back_end.services;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
public class DoctorService {

    private static final DateTimeFormatter TWELVE_HOUR_TIME =
            DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final TokenService tokenService;

    public DoctorService(
            DoctorRepository doctorRepository,
            AppointmentRepository appointmentRepository,
            TokenService tokenService) {
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.tokenService = tokenService;
    }

    @Transactional(readOnly = true)
    public List<String> getDoctorAvailability(Long doctorId, LocalDate date) {
        if (doctorId == null || date == null) {
            return Collections.emptyList();
        }

        Optional<Doctor> doctor = doctorRepository.findById(doctorId);
        if (doctor.isEmpty() || doctor.get().getAvailableTimes() == null) {
            return Collections.emptyList();
        }

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay().minusNanos(1);
        List<Appointment> appointments = appointmentRepository
                .findByDoctorIdAndAppointmentTimeBetween(doctorId, start, end);

        Set<LocalTime> bookedStartTimes = new HashSet<>();
        for (Appointment appointment : appointments) {
            if (appointment.getAppointmentTime() != null) {
                bookedStartTimes.add(appointment.getAppointmentTime().toLocalTime());
            }
        }

        return doctor.get().getAvailableTimes().stream()
                .filter(Objects::nonNull)
                .filter(slot -> parseSlotStart(slot)
                        .map(startTime -> !bookedStartTimes.contains(startTime))
                        .orElse(false))
                .toList();
    }

    @Transactional
    public int saveDoctor(Doctor doctor) {
        try {
            if (doctor == null || doctor.getEmail() == null) {
                return 0;
            }
            if (doctorRepository.findByEmail(doctor.getEmail()) != null) {
                return -1;
            }

            doctorRepository.save(doctor);
            return 1;
        } catch (Exception exception) {
            return 0;
        }
    }

    @Transactional
    public int updateDoctor(Doctor doctor) {
        try {
            if (doctor == null || doctor.getId() == null
                    || !doctorRepository.existsById(doctor.getId())) {
                return -1;
            }

            doctorRepository.save(doctor);
            return 1;
        } catch (Exception exception) {
            return 0;
        }
    }

    @Transactional(readOnly = true)
    public List<Doctor> getDoctors() {
        List<Doctor> doctors = doctorRepository.findAll();
        initializeAvailableTimes(doctors);
        return doctors;
    }

    @Transactional
    public int deleteDoctor(long id) {
        try {
            if (!doctorRepository.existsById(id)) {
                return -1;
            }

            appointmentRepository.deleteAllByDoctorId(id);
            doctorRepository.deleteById(id);
            return 1;
        } catch (Exception exception) {
            return 0;
        }
    }

    public ResponseEntity<Map<String, String>> validateDoctor(Login login) {
        if (login == null || login.getIdentifier() == null || login.getPassword() == null) {
            return response(HttpStatus.BAD_REQUEST, "Email and password are required.");
        }

        try {
            Doctor doctor = doctorRepository.findByEmail(login.getIdentifier());
            if (doctor == null || !Objects.equals(doctor.getPassword(), login.getPassword())) {
                return response(HttpStatus.UNAUTHORIZED, "Invalid credentials.");
            }

            String token = tokenService.generateToken(doctor.getEmail());
            return ResponseEntity.ok(Map.of("token", token));
        } catch (Exception exception) {
            return response(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to validate doctor credentials.");
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> findDoctorByName(String name) {
        List<Doctor> doctors = doctorRepository.findByNameLike(normalizeText(name));
        initializeAvailableTimes(doctors);
        return doctorResult(doctors);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorsByNameSpecilityandTime(
            String name, String specialty, String amOrPm) {
        List<Doctor> doctors = doctorRepository
                .findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(
                        normalizeText(name), normalizeText(specialty));
        return doctorResult(filterDoctorByTime(doctors, amOrPm));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorByNameAndTime(String name, String amOrPm) {
        List<Doctor> doctors = doctorRepository.findByNameLike(normalizeText(name));
        return doctorResult(filterDoctorByTime(doctors, amOrPm));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorByNameAndSpecility(String name, String specilty) {
        List<Doctor> doctors = doctorRepository
                .findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(
                        normalizeText(name), normalizeText(specilty));
        initializeAvailableTimes(doctors);
        return doctorResult(doctors);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorByTimeAndSpecility(String specilty, String amOrPm) {
        List<Doctor> doctors = doctorRepository.findBySpecialtyIgnoreCase(normalizeText(specilty));
        return doctorResult(filterDoctorByTime(doctors, amOrPm));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorBySpecility(String specilty) {
        List<Doctor> doctors = doctorRepository.findBySpecialtyIgnoreCase(normalizeText(specilty));
        initializeAvailableTimes(doctors);
        return doctorResult(doctors);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorsByTime(String amOrPm) {
        return doctorResult(filterDoctorByTime(doctorRepository.findAll(), amOrPm));
    }

    private List<Doctor> filterDoctorByTime(List<Doctor> doctors, String amOrPm) {
        String period = normalizeText(amOrPm).toUpperCase(Locale.ROOT);
        if (period.isEmpty()) {
            initializeAvailableTimes(doctors);
            return doctors;
        }
        if (!"AM".equals(period) && !"PM".equals(period)) {
            return Collections.emptyList();
        }

        return doctors.stream()
                .filter(doctor -> doctor.getAvailableTimes() != null)
                .filter(doctor -> doctor.getAvailableTimes().stream()
                        .filter(Objects::nonNull)
                        .map(this::parseSlotStart)
                        .flatMap(Optional::stream)
                        .anyMatch(time -> "AM".equals(period)
                                ? time.getHour() < 12
                                : time.getHour() >= 12))
                .toList();
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

    private void initializeAvailableTimes(List<Doctor> doctors) {
        doctors.forEach(doctor -> {
            if (doctor.getAvailableTimes() != null) {
                doctor.getAvailableTimes().size();
            }
        });
    }

    private Map<String, Object> doctorResult(List<Doctor> doctors) {
        return Map.of("doctors", doctors);
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private ResponseEntity<Map<String, String>> response(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("message", message));
    }
}
