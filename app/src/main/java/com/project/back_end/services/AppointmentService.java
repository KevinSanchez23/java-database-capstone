package com.project.back_end.services;

import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final TokenService tokenService;
    private final com.project.back_end.services.Service service;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            PatientRepository patientRepository,
            DoctorRepository doctorRepository,
            TokenService tokenService,
            com.project.back_end.services.Service service) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.tokenService = tokenService;
        this.service = service;
    }

    @Transactional
    public int bookAppointment(Appointment appointment) {
        try {
            appointmentRepository.save(appointment);
            return 1;
        } catch (Exception exception) {
            return 0;
        }
    }

    @Transactional
    public ResponseEntity<Map<String, String>> updateAppointment(Appointment appointment) {
        if (appointment == null || appointment.getId() == null) {
            return response(HttpStatus.BAD_REQUEST, "Appointment ID is required.");
        }

        Optional<Appointment> storedAppointment = appointmentRepository.findById(appointment.getId());
        if (storedAppointment.isEmpty()) {
            return response(HttpStatus.NOT_FOUND, "Appointment not found.");
        }

        Appointment currentAppointment = storedAppointment.get();
        if (currentAppointment.getPatient() == null
                || appointment.getPatient() == null
                || !Objects.equals(currentAppointment.getPatient().getId(), appointment.getPatient().getId())) {
            return response(HttpStatus.FORBIDDEN, "The appointment does not belong to this patient.");
        }

        if (appointment.getDoctor() == null || appointment.getDoctor().getId() == null) {
            return response(HttpStatus.BAD_REQUEST, "Doctor ID is required.");
        }

        Optional<Doctor> doctor = doctorRepository.findById(appointment.getDoctor().getId());
        if (doctor.isEmpty()) {
            return response(HttpStatus.BAD_REQUEST, "Invalid doctor ID.");
        }

        Optional<Patient> patient = patientRepository.findById(appointment.getPatient().getId());
        if (patient.isEmpty()) {
            return response(HttpStatus.BAD_REQUEST, "Invalid patient ID.");
        }

        appointment.setDoctor(doctor.get());
        appointment.setPatient(patient.get());

        int validationResult = service.validateAppointment(appointment);
        if (validationResult == -1) {
            return response(HttpStatus.BAD_REQUEST, "Invalid doctor ID.");
        }
        if (validationResult == 0) {
            return response(HttpStatus.CONFLICT, "The selected appointment time is not available.");
        }

        try {
            appointmentRepository.save(appointment);
            return response(HttpStatus.OK, "Appointment updated successfully.");
        } catch (Exception exception) {
            return response(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to update the appointment.");
        }
    }

    @Transactional
    public ResponseEntity<Map<String, String>> cancelAppointment(long id, String token) {
        try {
            String patientEmail = tokenService.extractIdentifier(token);
            Patient patient = patientRepository.findByEmail(patientEmail);
            if (patient == null) {
                return response(HttpStatus.UNAUTHORIZED, "Invalid patient token.");
            }

            Optional<Appointment> storedAppointment = appointmentRepository.findById(id);
            if (storedAppointment.isEmpty()) {
                return response(HttpStatus.NOT_FOUND, "Appointment not found.");
            }

            Appointment appointment = storedAppointment.get();
            if (appointment.getPatient() == null
                    || !Objects.equals(appointment.getPatient().getId(), patient.getId())) {
                return response(HttpStatus.FORBIDDEN, "You cannot cancel another patient's appointment.");
            }

            appointmentRepository.delete(appointment);
            return response(HttpStatus.OK, "Appointment canceled successfully.");
        } catch (Exception exception) {
            return response(HttpStatus.UNAUTHORIZED, "Invalid or expired token.");
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAppointment(String pname, LocalDate date, String token) {
        Map<String, Object> result = new LinkedHashMap<>();

        if (date == null) {
            result.put("appointments", Collections.emptyList());
            result.put("message", "Appointment date is required.");
            return result;
        }

        try {
            String doctorEmail = tokenService.extractIdentifier(token);
            Doctor doctor = doctorRepository.findByEmail(doctorEmail);
            if (doctor == null) {
                result.put("appointments", Collections.emptyList());
                result.put("message", "Invalid doctor token.");
                return result;
            }

            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay().minusNanos(1);
            boolean filterByPatient = pname != null
                    && !pname.isBlank()
                    && !"null".equalsIgnoreCase(pname);

            List<Appointment> appointments = filterByPatient
                    ? appointmentRepository
                            .findByDoctorIdAndPatient_NameContainingIgnoreCaseAndAppointmentTimeBetween(
                                    doctor.getId(), pname.trim(), start, end)
                    : appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(
                            doctor.getId(), start, end);

            result.put("appointments", appointments);
        } catch (Exception exception) {
            result.put("appointments", Collections.emptyList());
            result.put("message", "Unable to retrieve appointments.");
        }

        return result;
    }

    private ResponseEntity<Map<String, String>> response(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("message", message));
    }
}
