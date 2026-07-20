// patientDashboard.js

import { createDoctorCard } from "./components/doctorCard.js";
import { openModal } from "./components/modals.js";
import { getDoctors, filterDoctors } from "./services/doctorServices.js";
import { patientLogin, patientSignup } from "./services/patientServices.js";

document.addEventListener("DOMContentLoaded", () => {
  bindPatientModalButtons();
  bindDoctorFilters();
  loadDoctorCards();
});

function bindPatientModalButtons() {
  const signupButton = document.getElementById("patientSignup");
  const loginButton = document.getElementById("patientLogin");

  if (signupButton && signupButton.dataset.modalListenerAttached !== "true") {
    signupButton.dataset.modalListenerAttached = "true";
    signupButton.addEventListener("click", () => openModal("patientSignup"));
  }

  if (loginButton && loginButton.dataset.modalListenerAttached !== "true") {
    loginButton.dataset.modalListenerAttached = "true";
    loginButton.addEventListener("click", () => openModal("patientLogin"));
  }
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
  const name = document.getElementById("searchBar")?.value.trim() || null;
  const time = document.getElementById("filterTime")?.value || null;
  const specialty = document.getElementById("filterSpecialty")?.value || null;

  try {
    const response = await filterDoctors(name, time, specialty);
    const doctors = Array.isArray(response) ? response : response.doctors;
    renderDoctorCards(doctors, "No doctors found with the given filters.");
  } catch (error) {
    console.error("Failed to filter doctors:", error);
    renderDoctorCards([], "Unable to filter doctors. Please try again.");
  }
}

export function renderDoctorCards(doctors, emptyMessage = "No doctors found.") {
  const contentDiv = document.getElementById("content");

  if (!contentDiv) {
    return;
  }

  contentDiv.innerHTML = "";

  if (!Array.isArray(doctors) || doctors.length === 0) {
    const message = document.createElement("p");
    message.classList.add("no-doctors-message");
    message.textContent = emptyMessage;
    contentDiv.appendChild(message);
    return;
  }

  doctors.forEach((doctor) => {
    contentDiv.appendChild(createDoctorCard(doctor));
  });
}

window.signupPatient = async function signupPatient() {
  const name = document.getElementById("name")?.value.trim();
  const email = document.getElementById("email")?.value.trim();
  const password = document.getElementById("password")?.value;
  const phone = document.getElementById("phone")?.value.trim();
  const address = document.getElementById("address")?.value.trim();

  if (!name || !email || !password || !phone || !address) {
    alert("Please complete all required fields.");
    return;
  }

  const signupButton = document.getElementById("signupBtn");

  if (signupButton) {
    signupButton.disabled = true;
    signupButton.textContent = "Signing up...";
  }

  try {
    const result = await patientSignup({ name, email, password, phone, address });

    if (!result.success) {
      alert(result.message || "Patient signup failed.");
      return;
    }

    alert(result.message || "Patient registered successfully.");
    closePatientModal();
    window.location.reload();
  } catch (error) {
    console.error("Patient signup failed:", error);
    alert("Unable to complete signup. Please try again.");
  } finally {
    if (signupButton?.isConnected) {
      signupButton.disabled = false;
      signupButton.textContent = "Signup";
    }
  }
};

window.loginPatient = async function loginPatient() {
  const email = document.getElementById("email")?.value.trim();
  const password = document.getElementById("password")?.value;

  if (!email || !password) {
    alert("Email and password are required.");
    return;
  }

  const loginButton = document.getElementById("loginBtn");

  if (loginButton) {
    loginButton.disabled = true;
    loginButton.textContent = "Logging in...";
  }

  try {
    const response = await patientLogin({ email, password });
    const result = await readJsonResponse(response);

    if (!response.ok || !result.token) {
      alert(result.message || "Invalid credentials!");
      return;
    }

    localStorage.setItem("token", result.token);
    selectRole("loggedPatient");
    window.location.href = "/pages/loggedPatientDashboard.html";
  } catch (error) {
    console.error("Patient login failed:", error);
    alert("Unable to log in. Please try again.");
  } finally {
    if (loginButton?.isConnected) {
      loginButton.disabled = false;
      loginButton.textContent = "Login";
    }
  }
};

async function readJsonResponse(response) {
  try {
    return await response.json();
  } catch {
    return {};
  }
}

function closePatientModal() {
  const modal = document.getElementById("modal");

  if (modal) {
    modal.style.display = "none";
    modal.setAttribute("aria-hidden", "true");
  }
}

window.loadPatientDoctorCards = loadDoctorCards;
