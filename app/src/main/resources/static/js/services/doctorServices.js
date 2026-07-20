// doctorServices.js

import { API_BASE_URL } from "../config/config.js";

const DOCTOR_API = API_BASE_URL + "/doctor";

async function readResponseBody(response) {
  try {
    return await response.json();
  } catch {
    return {};
  }
}

export async function getDoctors() {
  try {
    const response = await fetch(DOCTOR_API, {
      method: "GET",
      headers: {
        Accept: "application/json",
      },
    });

    const data = await readResponseBody(response);

    if (!response.ok) {
      throw new Error(data.message || `Unable to load doctors (${response.status}).`);
    }

    if (Array.isArray(data)) {
      return data;
    }

    return Array.isArray(data.doctors) ? data.doctors : [];
  } catch (error) {
    console.error("Failed to fetch doctors:", error);
    return [];
  }
}

export async function deleteDoctor(id, token) {
  if (id === null || id === undefined || !token) {
    return {
      success: false,
      message: "Doctor ID and authentication token are required.",
    };
  }

  try {
    const endpoint = `${DOCTOR_API}/${encodeURIComponent(id)}/${encodeURIComponent(token)}`;
    const response = await fetch(endpoint, {
      method: "DELETE",
      headers: {
        Accept: "application/json",
      },
    });

    const data = await readResponseBody(response);

    return {
      success: response.ok,
      message:
        data.message ||
        (response.ok ? "Doctor deleted successfully." : "The doctor could not be deleted."),
    };
  } catch (error) {
    console.error("Failed to delete doctor:", error);
    return {
      success: false,
      message: "A network error occurred while deleting the doctor.",
    };
  }
}

export async function saveDoctor(doctor, token) {
  if (!doctor || !token) {
    return {
      success: false,
      message: "Doctor data and authentication token are required.",
    };
  }

  try {
    const endpoint = `${DOCTOR_API}/${encodeURIComponent(token)}`;
    const response = await fetch(endpoint, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Accept: "application/json",
      },
      body: JSON.stringify(doctor),
    });

    const data = await readResponseBody(response);

    return {
      success: response.ok,
      message:
        data.message ||
        (response.ok ? "Doctor added successfully." : "The doctor could not be added."),
    };
  } catch (error) {
    console.error("Failed to save doctor:", error);
    return {
      success: false,
      message: "A network error occurred while saving the doctor.",
    };
  }
}

export async function filterDoctors(name, time, specialty) {
  const normalizeFilter = (value) => {
    if (value === null || value === undefined || String(value).trim() === "") {
      return "null";
    }

    return encodeURIComponent(String(value).trim());
  };

  const endpoint = `${DOCTOR_API}/filter/${normalizeFilter(name)}/${normalizeFilter(time)}/${normalizeFilter(specialty)}`;

  try {
    const response = await fetch(endpoint, {
      method: "GET",
      headers: {
        Accept: "application/json",
      },
    });

    const data = await readResponseBody(response);

    if (!response.ok) {
      console.error(data.message || `Unable to filter doctors (${response.status}).`);
      return { doctors: [] };
    }

    if (Array.isArray(data)) {
      return { doctors: data };
    }

    return {
      ...data,
      doctors: Array.isArray(data.doctors) ? data.doctors : [],
    };
  } catch (error) {
    console.error("Failed to filter doctors:", error);
    alert("Something went wrong while filtering doctors.");
    return { doctors: [] };
  }
}
