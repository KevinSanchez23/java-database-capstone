// index.js

import { openModal } from "../components/modals.js";
import { API_BASE_URL } from "../config/config.js";

const ADMIN_API = API_BASE_URL + "/admin";
const DOCTOR_API = API_BASE_URL + "/doctor/login";

window.onload = function () {
  const adminBtn = document.getElementById("adminLogin");
  const doctorBtn = document.getElementById("doctorLogin");

  if (adminBtn) {
    adminBtn.addEventListener("click", () => {
      openModal("adminLogin");
    });
  }

  if (doctorBtn) {
    doctorBtn.addEventListener("click", () => {
      openModal("doctorLogin");
    });
  }
};

window.adminLoginHandler = async function adminLoginHandler() {
  const usernameInput = document.getElementById("username");
  const passwordInput = document.getElementById("password");

  const username = usernameInput?.value.trim();
  const password = passwordInput?.value;

  if (!username || !password) {
    alert("Username and password are required.");
    return;
  }

  const admin = { username, password };

  try {
    const response = await fetch(ADMIN_API, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(admin),
    });

    if (!response.ok) {
      alert("Invalid credentials!");
      return;
    }

    const result = await response.json();

    if (!result.token) {
      throw new Error("The server did not return an authentication token.");
    }

    localStorage.setItem("token", result.token);
    selectRole("admin");
  } catch (error) {
    console.error("Admin login failed:", error);
    alert("Unable to log in. Please try again.");
  }
};

window.doctorLoginHandler = async function doctorLoginHandler() {
  const emailInput = document.getElementById("email");
  const passwordInput = document.getElementById("password");

  const email = emailInput?.value.trim();
  const password = passwordInput?.value;

  if (!email || !password) {
    alert("Email and password are required.");
    return;
  }

  const doctor = { email, password };

  try {
    const response = await fetch(DOCTOR_API, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(doctor),
    });

    if (!response.ok) {
      alert("Invalid credentials!");
      return;
    }

    const result = await response.json();

    if (!result.token) {
      throw new Error("The server did not return an authentication token.");
    }

    localStorage.setItem("token", result.token);
    selectRole("doctor");
  } catch (error) {
    console.error("Doctor login failed:", error);
    alert("Unable to log in. Please try again.");
  }
};
