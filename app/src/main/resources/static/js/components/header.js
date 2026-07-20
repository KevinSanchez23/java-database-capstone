// header.js

const headerScriptUrl = document.currentScript?.src || "/js/components/header.js";
const modalModuleUrl = new URL("./modals.js", headerScriptUrl).href;

function isHomePage() {
  const path = window.location.pathname;
  return path === "/" || path.endsWith("/index.html");
}

async function openHeaderModal(type) {
  try {
    const { openModal } = await import(modalModuleUrl);
    openModal(type);
  } catch (error) {
    console.error(`Unable to open the ${type} modal:`, error);
    alert("The requested form could not be opened. Please try again.");
  }
}

function renderHeader() {
  const headerDiv = document.getElementById("header");

  if (!headerDiv) {
    return;
  }

  if (isHomePage()) {
    localStorage.removeItem("userRole");
    localStorage.removeItem("token");

    headerDiv.innerHTML = `
      <header class="header">
        <a class="logo-link" href="/" aria-label="Smart Clinic home">
          <img src="/assets/images/logo/logo.png" alt="Smart Clinic logo" class="logo-img">
          <span class="logo-title">Smart Clinic</span>
        </a>
      </header>
    `;
    return;
  }

  const role = localStorage.getItem("userRole");
  const token = localStorage.getItem("token");
  const protectedRoles = ["loggedPatient", "admin", "doctor"];

  if (protectedRoles.includes(role) && !token) {
    localStorage.removeItem("userRole");
    alert("Session expired or invalid login. Please log in again.");
    window.location.href = "/";
    return;
  }

  let headerContent = `
    <header class="header">
      <a class="logo-link" href="/" aria-label="Smart Clinic home">
        <img src="/assets/images/logo/logo.png" alt="Smart Clinic logo" class="logo-img">
        <span class="logo-title">Smart Clinic</span>
      </a>
      <nav aria-label="Main navigation">
  `;

  if (role === "admin") {
    headerContent += `
      <button type="button" id="addDocBtn" class="adminBtn">Add Doctor</button>
      <a href="#" id="logoutBtn">Logout</a>
    `;
  } else if (role === "doctor") {
    headerContent += `
      <button type="button" id="doctorHomeBtn" class="adminBtn">Home</button>
      <a href="#" id="logoutBtn">Logout</a>
    `;
  } else if (role === "patient") {
    headerContent += `
      <button type="button" id="patientLogin" class="adminBtn">Login</button>
      <button type="button" id="patientSignup" class="adminBtn">Sign Up</button>
    `;
  } else if (role === "loggedPatient") {
    headerContent += `
      <button type="button" id="patientHomeBtn" class="adminBtn">Home</button>
      <button type="button" id="patientAppointmentsBtn" class="adminBtn">Appointments</button>
      <a href="#" id="logoutPatientBtn">Logout</a>
    `;
  }

  headerContent += `
      </nav>
    </header>
  `;

  headerDiv.innerHTML = headerContent;
  attachHeaderButtonListeners();
}

function attachHeaderButtonListeners() {
  const addDoctorButton = document.getElementById("addDocBtn");
  const patientLoginButton = document.getElementById("patientLogin");
  const patientSignupButton = document.getElementById("patientSignup");
  const doctorHomeButton = document.getElementById("doctorHomeBtn");
  const patientHomeButton = document.getElementById("patientHomeBtn");
  const patientAppointmentsButton = document.getElementById("patientAppointmentsBtn");
  const logoutButton = document.getElementById("logoutBtn");
  const logoutPatientButton = document.getElementById("logoutPatientBtn");

  if (addDoctorButton && addDoctorButton.dataset.modalListenerAttached !== "true") {
    addDoctorButton.dataset.modalListenerAttached = "true";
    addDoctorButton.addEventListener("click", () => openHeaderModal("addDoctor"));
  }
  if (patientLoginButton && patientLoginButton.dataset.modalListenerAttached !== "true") {
    patientLoginButton.dataset.modalListenerAttached = "true";
    patientLoginButton.addEventListener("click", () => openHeaderModal("patientLogin"));
  }

  if (patientSignupButton && patientSignupButton.dataset.modalListenerAttached !== "true") {
    patientSignupButton.dataset.modalListenerAttached = "true";
    patientSignupButton.addEventListener("click", () => openHeaderModal("patientSignup"));
  }

  doctorHomeButton?.addEventListener("click", () => {
    const token = localStorage.getItem("token");
    window.location.href = `/doctorDashboard/${encodeURIComponent(token)}`;
  });

  patientHomeButton?.addEventListener("click", () => {
    window.location.href = "/pages/loggedPatientDashboard.html";
  });

  patientAppointmentsButton?.addEventListener("click", () => {
    window.location.href = "/pages/patientAppointments.html";
  });

  logoutButton?.addEventListener("click", (event) => {
    event.preventDefault();
    logout();
  });

  logoutPatientButton?.addEventListener("click", (event) => {
    event.preventDefault();
    logoutPatient();
  });
}

function logout() {
  localStorage.removeItem("token");
  localStorage.removeItem("userRole");
  window.location.href = "/";
}

function logoutPatient() {
  localStorage.removeItem("token");
  localStorage.setItem("userRole", "patient");
  window.location.href = "/pages/patientDashboard.html";
}

window.renderHeader = renderHeader;
window.attachHeaderButtonListeners = attachHeaderButtonListeners;
window.logout = logout;
window.logoutPatient = logoutPatient;

renderHeader();
