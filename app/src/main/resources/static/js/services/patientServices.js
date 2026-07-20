// patientServices.js

import { API_BASE_URL } from "../config/config.js";

const PATIENT_API = API_BASE_URL + "/patient";

async function readResponseBody(response) {
  try {
    return await response.json();
  } catch {
    return {};
  }
}

export async function patientSignup(data) {
  // Send the patient registration data to the backend.
  if (!data) {
    return { success: false, message: "Patient data is required." };
  }

  try {
    const response = await fetch(PATIENT_API, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Accept: "application/json",
      },
      body: JSON.stringify(data),
    });

    const result = await readResponseBody(response);

    return {
      success: response.ok,
      message:
        result.message ||
        (response.ok ? "Patient registered successfully." : "Patient registration failed."),
    };
  } catch (error) {
    console.error("Patient signup failed:", error);
    return {
      success: false,
      message: "A network error occurred while registering the patient.",
    };
  }
}

export async function patientLogin(data) {
  // Return the complete Response so the caller can inspect status and token data.
  if (!data) {
    throw new Error("Patient login data is required.");
  }

  try {
    return await fetch(`${PATIENT_API}/login`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Accept: "application/json",
      },
      body: JSON.stringify(data),
    });
  } catch (error) {
    console.error("Patient login request failed:", error);
    throw error;
  }
}

export async function getPatientData(token) {
  // Retrieve the patient associated with the current authentication token.
  if (!token) {
    console.error("A token is required to fetch patient data.");
    return null;
  }

  try {
    const endpoint = `${PATIENT_API}/${encodeURIComponent(token)}`;
    const response = await fetch(endpoint, {
      method: "GET",
      headers: {
        Accept: "application/json",
      },
    });

    const data = await readResponseBody(response);

    if (!response.ok) {
      console.error(data.message || `Unable to fetch patient data (${response.status}).`);
      return null;
    }

    return data.patient || null;
  } catch (error) {
    console.error("Failed to fetch patient data:", error);
    return null;
  }
}

export async function getPatientAppointments(id, token, user) {
  // Use one role-aware endpoint for both patient and doctor dashboards.
  if (id === null || id === undefined || !token || !user) {
    console.error("Patient ID, token, and user role are required to fetch appointments.");
    return null;
  }

  try {
    const endpoint = `${PATIENT_API}/${encodeURIComponent(id)}/${encodeURIComponent(user)}/${encodeURIComponent(token)}`;
    const response = await fetch(endpoint, {
      method: "GET",
      headers: {
        Accept: "application/json",
      },
    });

    const data = await readResponseBody(response);

    if (!response.ok) {
      console.error(data.message || `Unable to fetch appointments (${response.status}).`);
      return null;
    }

    return Array.isArray(data.appointments) ? data.appointments : [];
  } catch (error) {
    console.error("Failed to fetch patient appointments:", error);
    return null;
  }
}

export async function filterAppointments(condition, name, token) {
  // Represent empty route filters as the literal value expected by the backend.
  if (!token) {
    console.error("A token is required to filter appointments.");
    return { appointments: [] };
  }

  const normalizeFilter = (value) => {
    if (value === null || value === undefined || String(value).trim() === "") {
      return "null";
    }

    return encodeURIComponent(String(value).trim());
  };

  const endpoint = `${PATIENT_API}/filter/${normalizeFilter(condition)}/${normalizeFilter(name)}/${encodeURIComponent(token)}`;

  try {
    const response = await fetch(endpoint, {
      method: "GET",
      headers: {
        Accept: "application/json",
      },
    });

    const data = await readResponseBody(response);

    if (!response.ok) {
      console.error(data.message || `Unable to filter appointments (${response.status}).`);
      return { appointments: [] };
    }

    if (Array.isArray(data)) {
      return { appointments: data };
    }

    return {
      ...data,
      appointments: Array.isArray(data.appointments) ? data.appointments : [],
    };
  } catch (error) {
    console.error("Failed to filter appointments:", error);
    alert("Something went wrong while filtering appointments.");
    return { appointments: [] };
  }
}
