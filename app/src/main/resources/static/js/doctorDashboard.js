// doctorDashboard.js

import { getAllAppointments } from "./services/appointmentRecordService.js";
import { createPatientRow } from "./components/patientRows.js";

let patientTableBody = null;
let selectedDate = getLocalToday();
const token = localStorage.getItem("token");
let patientName = null;

function getLocalToday() {
  const today = new Date();
  const year = today.getFullYear();
  const month = String(today.getMonth() + 1).padStart(2, "0");
  const day = String(today.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

document.addEventListener("DOMContentLoaded", () => {
  patientTableBody = document.getElementById("patientTableBody");

  const searchBar = document.getElementById("searchBar");
  const todayButton = document.getElementById("todayButton");
  const datePicker = document.getElementById("datePicker");

  if (typeof window.renderContent === "function") {
    window.renderContent();
  }

  if (datePicker) {
    datePicker.value = selectedDate;
  }

  searchBar?.addEventListener("input", () => {
    const searchValue = searchBar.value.trim();
    patientName = searchValue || "null";
    loadAppointments();
  });

  todayButton?.addEventListener("click", () => {
    selectedDate = getLocalToday();

    if (datePicker) {
      datePicker.value = selectedDate;
    }

    loadAppointments();
  });

  datePicker?.addEventListener("change", () => {
    selectedDate = datePicker.value || getLocalToday();
    datePicker.value = selectedDate;
    loadAppointments();
  });

  loadAppointments();
});

async function loadAppointments() {
  if (!patientTableBody) {
    return;
  }

  if (!token) {
    renderMessageRow("Session expired or invalid login. Please log in again.");
    return;
  }

  renderMessageRow("Loading appointments...");

  try {
    const response = await getAllAppointments(selectedDate, patientName, token);
    const appointments = Array.isArray(response) ? response : response?.appointments || [];

    patientTableBody.innerHTML = "";

    if (appointments.length === 0) {
      renderMessageRow("No Appointments found for today.");
      return;
    }

    appointments.forEach((appointment) => {
      const patient = appointment.patient || {
        id: appointment.patientId,
        name: appointment.patientName,
        phone: appointment.patientPhone,
        email: appointment.patientEmail,
      };

      const appointmentId = appointment.id;
      const doctorId = appointment.doctorId ?? appointment.doctor?.id;
      const row = createPatientRow(patient, appointmentId, doctorId);
      patientTableBody.appendChild(row);
    });
  } catch (error) {
    console.error("Failed to load appointments:", error);
    renderMessageRow("Error loading appointments. Try again later.");
  }
}

function renderMessageRow(message) {
  if (!patientTableBody) {
    return;
  }

  patientTableBody.innerHTML = "";

  const row = document.createElement("tr");
  const cell = document.createElement("td");
  cell.colSpan = 5;
  cell.classList.add("noPatientRecord");
  cell.textContent = message;
  row.appendChild(cell);
  patientTableBody.appendChild(row);
}

window.loadAppointments = loadAppointments;
