// doctorCard.js

import { getPatientData } from "../services/patientServices.js";

export function createDoctorCard(doctor) {
  const card = document.createElement("div");
  card.classList.add("doctor-card");
  card.dataset.doctorId = doctor.id;

  const role = localStorage.getItem("userRole");

  const infoDiv = document.createElement("div");
  infoDiv.classList.add("doctor-info");

  const name = document.createElement("h3");
  name.textContent = doctor.name || "Unknown doctor";

  const specialization = document.createElement("p");
  specialization.textContent = `Specialty: ${doctor.specialty || "Not specified"}`;

  const email = document.createElement("p");
  email.textContent = `Email: ${doctor.email || "Not provided"}`;

  const availability = document.createElement("p");
  const availableTimes = Array.isArray(doctor.availableTimes)
    ? doctor.availableTimes.join(", ")
    : "";
  availability.textContent = `Availability: ${availableTimes || "No available times"}`;

  infoDiv.appendChild(name);
  infoDiv.appendChild(specialization);
  infoDiv.appendChild(email);
  infoDiv.appendChild(availability);

  const actionsDiv = document.createElement("div");
  actionsDiv.classList.add("card-actions");

  if (role === "admin") {
    const removeBtn = document.createElement("button");
    removeBtn.type = "button";
    removeBtn.classList.add("delete-doctor-btn");
    removeBtn.textContent = "Delete";

    removeBtn.addEventListener("click", async () => {
      const shouldDelete = window.confirm(
        `Are you sure you want to delete ${doctor.name || "this doctor"}?`
      );

      if (!shouldDelete) {
        return;
      }

      const token = localStorage.getItem("token");
      if (!token) {
        alert("Session expired or invalid login. Please log in again.");
        return;
      }

      removeBtn.disabled = true;
      removeBtn.textContent = "Deleting...";

      try {
        const { deleteDoctor } = await import("../services/doctorServices.js");

        if (typeof deleteDoctor !== "function") {
          throw new Error("The delete doctor service is not available.");
        }

        const result = await deleteDoctor(doctor.id, token);

        if (result.success) {
          alert(result.message || "Doctor deleted successfully.");
          card.remove();
          return;
        }

        alert(result.message || "The doctor could not be deleted.");
      } catch (error) {
        console.error("Failed to delete doctor:", error);
        alert("The doctor could not be deleted. Please try again.");
      } finally {
        if (card.isConnected) {
          removeBtn.disabled = false;
          removeBtn.textContent = "Delete";
        }
      }
    });

    actionsDiv.appendChild(removeBtn);
  } else if (role === "patient") {
    const bookNow = document.createElement("button");
    bookNow.type = "button";
    bookNow.classList.add("book-now-btn");
    bookNow.textContent = "Book Now";

    bookNow.addEventListener("click", () => {
      alert("Patient needs to login first.");
    });

    actionsDiv.appendChild(bookNow);
  } else if (role === "loggedPatient") {
    const bookNow = document.createElement("button");
    bookNow.type = "button";
    bookNow.classList.add("book-now-btn");
    bookNow.textContent = "Book Now";

    bookNow.addEventListener("click", async (event) => {
      const token = localStorage.getItem("token");

      if (!token) {
        alert("Session expired or invalid login. Please log in again.");
        localStorage.setItem("userRole", "patient");
        window.location.href = "/pages/patientDashboard.html";
        return;
      }

      bookNow.disabled = true;

      try {
        const patientData = await getPatientData(token);

        if (!patientData) {
          throw new Error("Patient information could not be loaded.");
        }

        const { showBookingOverlay } = await import("../loggedPatient.js");
        showBookingOverlay(event, doctor, patientData);
      } catch (error) {
        console.error("Failed to start appointment booking:", error);
        alert("The booking form could not be opened. Please try again.");
      } finally {
        bookNow.disabled = false;
      }
    });

    actionsDiv.appendChild(bookNow);
  }

  card.appendChild(infoDiv);
  card.appendChild(actionsDiv);

  return card;
}
