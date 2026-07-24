package com.project.back_end.services;

import com.project.back_end.DTO.AppointmentDTO;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class PatientService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PatientService.class);

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final TokenService tokenService;

    public PatientService(
            PatientRepository patientRepository,
            AppointmentRepository appointmentRepository,
            DoctorRepository doctorRepository,
            TokenService tokenService) {
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
        this.tokenService = tokenService;
    }

    @Transactional
    public int createPatient(Patient patient) {
        try {
            patientRepository.save(patient);
            return 1;
        } catch (Exception exception) {
            LOGGER.error("Unable to create patient", exception);
            return 0;
        }
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getPatientAppointment(Long id, String token) {
        return getPatientAppointment(id, token, "patient");
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getPatientAppointment(
            Long id, String token, String user) {
        if (id == null) {
            return error(HttpStatus.BAD_REQUEST, "Patient ID is required.");
        }

        try {
            String role = user == null ? "" : user.trim().toLowerCase(Locale.ROOT);
            String email = tokenService.extractIdentifier(token);
            List<Appointment> storedAppointments;

            if ("patient".equals(role) || "loggedpatient".equals(role)) {
                Patient authenticatedPatient = patientRepository.findByEmail(email);
                if (authenticatedPatient == null
                        || !Objects.equals(authenticatedPatient.getId(), id)) {
                    return error(HttpStatus.FORBIDDEN,
                            "You are not authorized to access these appointments.");
                }
                storedAppointments = appointmentRepository.findByPatientId(id);
            } else if ("doctor".equals(role)) {
                Doctor authenticatedDoctor = doctorRepository.findByEmail(email);
                if (authenticatedDoctor == null) {
                    return error(HttpStatus.UNAUTHORIZED, "Invalid doctor token.");
                }
                storedAppointments = appointmentRepository.findByPatientIdAndDoctorId(
                        id, authenticatedDoctor.getId());
            } else {
                return error(HttpStatus.BAD_REQUEST, "Unsupported user role.");
            }

            List<AppointmentDTO> appointments = toAppointmentDTOs(storedAppointments);
            return appointmentsResponse(appointments);
        } catch (Exception exception) {
            LOGGER.error("Unable to retrieve appointments for patient {}", id, exception);
            return error(HttpStatus.UNAUTHORIZED, "Invalid or expired token.");
        }
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> filterByCondition(String condition, Long id) {
        Integer status = statusForCondition(condition);
        if (status == null) {
            return error(HttpStatus.BAD_REQUEST,
                    "Condition must be either 'past' or 'future'.");
        }
        if (id == null) {
            return error(HttpStatus.BAD_REQUEST, "Patient ID is required.");
        }

        try {
            List<AppointmentDTO> appointments = toAppointmentDTOs(
                    appointmentRepository
                            .findByPatient_IdAndStatusOrderByAppointmentTimeAsc(id, status));
            return appointmentsResponse(appointments);
        } catch (Exception exception) {
            LOGGER.error("Unable to filter appointments by condition", exception);
            return error(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to filter appointments.");
        }
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> filterByDoctor(String name, Long patientId) {
        if (patientId == null) {
            return error(HttpStatus.BAD_REQUEST, "Patient ID is required.");
        }

        try {
            List<AppointmentDTO> appointments = toAppointmentDTOs(
                    appointmentRepository.filterByDoctorNameAndPatientId(
                            normalizeName(name), patientId));
            return appointmentsResponse(appointments);
        } catch (Exception exception) {
            LOGGER.error("Unable to filter appointments by doctor", exception);
            return error(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to filter appointments.");
        }
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> filterByDoctorAndCondition(
            String condition, String name, long patientId) {
        Integer status = statusForCondition(condition);
        if (status == null) {
            return error(HttpStatus.BAD_REQUEST,
                    "Condition must be either 'past' or 'future'.");
        }

        try {
            List<AppointmentDTO> appointments = toAppointmentDTOs(
                    appointmentRepository.filterByDoctorNameAndPatientIdAndStatus(
                            normalizeName(name), patientId, status));
            return appointmentsResponse(appointments);
        } catch (Exception exception) {
            LOGGER.error("Unable to filter appointments by doctor and condition", exception);
            return error(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to filter appointments.");
        }
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getPatientDetails(String token) {
        try {
            String email = tokenService.extractIdentifier(token);
            Patient patient = patientRepository.findByEmail(email);
            if (patient == null) {
                return error(HttpStatus.NOT_FOUND, "Patient not found.");
            }

            Map<String, Object> patientDetails = new LinkedHashMap<>();
            patientDetails.put("id", patient.getId());
            patientDetails.put("name", patient.getName());
            patientDetails.put("email", patient.getEmail());
            patientDetails.put("phone", patient.getPhone());
            patientDetails.put("address", patient.getAddress());

            return ResponseEntity.ok(Map.of("patient", patientDetails));
        } catch (Exception exception) {
            LOGGER.error("Unable to retrieve patient details", exception);
            return error(HttpStatus.UNAUTHORIZED, "Invalid or expired token.");
        }
    }

    private List<AppointmentDTO> toAppointmentDTOs(List<Appointment> appointments) {
        return appointments.stream()
                .map(this::toAppointmentDTO)
                .toList();
    }

    private AppointmentDTO toAppointmentDTO(Appointment appointment) {
        return new AppointmentDTO(
                appointment.getId(),
                appointment.getDoctor().getId(),
                appointment.getDoctor().getName(),
                appointment.getPatient().getId(),
                appointment.getPatient().getName(),
                appointment.getPatient().getEmail(),
                appointment.getPatient().getPhone(),
                appointment.getPatient().getAddress(),
                appointment.getAppointmentTime(),
                appointment.getStatus());
    }

    private Integer statusForCondition(String condition) {
        if (condition == null) {
            return null;
        }

        return switch (condition.trim().toLowerCase(Locale.ROOT)) {
            case "past" -> 1;
            case "future" -> 0;
            default -> null;
        };
    }

    private String normalizeName(String name) {
        return name == null ? "" : name.trim();
    }

    private ResponseEntity<Map<String, Object>> appointmentsResponse(
            List<AppointmentDTO> appointments) {
        return ResponseEntity.ok(Map.of("appointments", appointments));
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("message", message));
    }
}
