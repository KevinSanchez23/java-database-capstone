// adminDashboard.js

import { openModal } from "./components/modals.js";
import { getDoctors, filterDoctors, saveDoctor } from "./services/doctorServices.js";
import { createDoctorCard } from "./components/doctorCard.js";

document.addEventListener("DOMContentLoaded", () => {
  bindAddDoctorButton();
  bindDoctorFilters();
  loadDoctorCards();
});

function bindAddDoctorButton() {
  const addDoctorButton = document.getElementById("addDocBtn");

  if (!addDoctorButton || addDoctorButton.dataset.modalListenerAttached === "true") {
    return;
  }

  addDoctorButton.dataset.modalListenerAttached = "true";
  addDoctorButton.addEventListener("click", () => {
    openModal("addDoctor");
  });
}

function bindDoctorFilters() {
  document.getElementById("searchBar")?.addEventListener("input", filterDoctorsOnChange);
  document.getElementById("filterTime")?.addEventListener("change", filterDoctorsOnChange);
  document
    .getElementById("filterSpecialty")
    ?.addEventListener("change", filterDoctorsOnChange);
}

async function loadDoctorCards() {
  const contentDiv = document.getElementById("content");

  if (!contentDiv) {
    return;
  }

  contentDiv.innerHTML = "<p>Loading doctors...</p>";

  try {
    const doctors = await getDoctors();
    renderDoctorCards(doctors);
  } catch (error) {
    console.error("Failed to load doctors:", error);
    contentDiv.innerHTML = '<p class="no-doctors-message">Unable to load doctors.</p>';
  }
}

async function filterDoctorsOnChange() {
  const searchBar = document.getElementById("searchBar");
  const timeFilter = document.getElementById("filterTime");
  const specialtyFilter = document.getElementById("filterSpecialty");

  const name = searchBar?.value.trim() || null;
  const time = timeFilter?.value || null;
  const specialty = specialtyFilter?.value || null;

  try {
    const response = await filterDoctors(name, time, specialty);
    const doctors = Array.isArray(response) ? response : response.doctors;
    renderDoctorCards(doctors);
  } catch (error) {
    console.error("Failed to filter doctors:", error);
    renderDoctorCards([]);
  }
}

function renderDoctorCards(doctors) {
  const contentDiv = document.getElementById("content");

  if (!contentDiv) {
    return;
  }

  contentDiv.innerHTML = "";

  if (!Array.isArray(doctors) || doctors.length === 0) {
    contentDiv.innerHTML = '<p class="no-doctors-message">No doctors found</p>';
    return;
  }

  doctors.forEach((doctor) => {
    contentDiv.appendChild(createDoctorCard(doctor));
  });
}

window.adminAddDoctor = async function adminAddDoctor() {
  const name = document.getElementById("doctorName")?.value.trim();
  const specialty = document.getElementById("specialization")?.value;
  const email = document.getElementById("doctorEmail")?.value.trim();
  const password = document.getElementById("doctorPassword")?.value;
  const phone = document.getElementById("doctorPhone")?.value.trim();
  const availableTimes = Array.from(
    document.querySelectorAll('input[name="availability"]:checked'),
    (checkbox) => checkbox.value
  );

  if (!name || !specialty || !email || !password || !phone) {
    alert("Please complete all required doctor fields.");
    return;
  }

  const token = localStorage.getItem("token");

  if (!token) {
    alert("Session expired or invalid login. Please log in again.");
    localStorage.removeItem("userRole");
    window.location.href = "/";
    return;
  }

  const doctor = {
    name,
    specialty,
    email,
    password,
    phone,
    availableTimes,
  };

  const saveButton = document.getElementById("saveDoctorBtn");

  if (saveButton) {
    saveButton.disabled = true;
    saveButton.textContent = "Saving...";
  }

  try {
    const result = await saveDoctor(doctor, token);

    if (!result.success) {
      alert(result.message || "The doctor could not be added.");
      return;
    }

    document.getElementById("modal").style.display = "none";
    alert(result.message || "Doctor added successfully.");
    await loadDoctorCards();
  } catch (error) {
    console.error("Failed to add doctor:", error);
    alert("The doctor could not be added. Please try again.");
  } finally {
    if (saveButton?.isConnected) {
      saveButton.disabled = false;
      saveButton.textContent = "Save";
    }
  }
};

window.loadDoctorCards = loadDoctorCards;
window.renderDoctorCards = renderDoctorCards;
