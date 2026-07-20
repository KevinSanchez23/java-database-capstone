package com.project.back_end.services;

import com.project.back_end.models.Prescription;
import com.project.back_end.repo.PrescriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class PrescriptionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrescriptionService.class);

    private final PrescriptionRepository prescriptionRepository;

    public PrescriptionService(PrescriptionRepository prescriptionRepository) {
        this.prescriptionRepository = prescriptionRepository;
    }

    public ResponseEntity<Map<String, String>> savePrescription(Prescription prescription) {
        if (prescription == null || prescription.getAppointmentId() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Appointment ID is required."));
        }

        try {
            List<Prescription> existingPrescriptions = prescriptionRepository
                    .findByAppointmentId(prescription.getAppointmentId());

            if (!existingPrescriptions.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message",
                                "Prescription already exists for this appointment."));
            }

            prescriptionRepository.save(prescription);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "Prescription saved"));
        } catch (Exception exception) {
            LOGGER.error("Unable to save prescription", exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "An error occurred while saving the prescription."));
        }
    }

    public ResponseEntity<Map<String, Object>> getPrescription(Long appointmentId) {
        if (appointmentId == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Appointment ID is required."));
        }

        try {
            List<Prescription> prescriptions = prescriptionRepository
                    .findByAppointmentId(appointmentId);
            return ResponseEntity.ok(Map.of("prescription", prescriptions));
        } catch (Exception exception) {
            LOGGER.error("Unable to retrieve prescription for appointment {}",
                    appointmentId, exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "An error occurred while retrieving the prescription."));
        }
    }
}
