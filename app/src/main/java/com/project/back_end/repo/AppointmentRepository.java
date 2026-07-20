package com.project.back_end.repo;

import com.project.back_end.models.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Query("""
            SELECT DISTINCT appointment
            FROM Appointment appointment
            LEFT JOIN FETCH appointment.doctor doctor
            LEFT JOIN FETCH doctor.availableTimes
            WHERE doctor.id = :doctorId
              AND appointment.appointmentTime BETWEEN :start AND :end
            ORDER BY appointment.appointmentTime ASC
            """)
    List<Appointment> findByDoctorIdAndAppointmentTimeBetween(
            @Param("doctorId") Long doctorId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("""
            SELECT DISTINCT appointment
            FROM Appointment appointment
            LEFT JOIN FETCH appointment.patient patient
            LEFT JOIN FETCH appointment.doctor doctor
            WHERE doctor.id = :doctorId
              AND LOWER(patient.name) LIKE LOWER(CONCAT('%', :patientName, '%'))
              AND appointment.appointmentTime BETWEEN :start AND :end
            ORDER BY appointment.appointmentTime ASC
            """)
    List<Appointment> findByDoctorIdAndPatient_NameContainingIgnoreCaseAndAppointmentTimeBetween(
            @Param("doctorId") Long doctorId,
            @Param("patientName") String patientName,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Modifying
    @Transactional
    void deleteAllByDoctorId(Long doctorId);

    List<Appointment> findByPatientId(Long patientId);

    List<Appointment> findByPatient_IdAndStatusOrderByAppointmentTimeAsc(Long patientId, int status);

    @Query("""
            SELECT appointment
            FROM Appointment appointment
            WHERE LOWER(appointment.doctor.name) LIKE LOWER(CONCAT('%', :doctorName, '%'))
              AND appointment.patient.id = :patientId
            """)
    List<Appointment> filterByDoctorNameAndPatientId(
            @Param("doctorName") String doctorName,
            @Param("patientId") Long patientId);

    @Query("""
            SELECT appointment
            FROM Appointment appointment
            WHERE LOWER(appointment.doctor.name) LIKE LOWER(CONCAT('%', :doctorName, '%'))
              AND appointment.patient.id = :patientId
              AND appointment.status = :status
            """)
    List<Appointment> filterByDoctorNameAndPatientIdAndStatus(
            @Param("doctorName") String doctorName,
            @Param("patientId") Long patientId,
            @Param("status") int status);
}
