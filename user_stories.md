# User Story Template

**Title:**
*As a [user role], I want [feature/goal], so that [reason].*

**Acceptance Criteria:**
1. [Criteria 1]
2. [Criteria 2]
3. [Criteria 3]

**Priority:** [High/Medium/Low]
**Story Points:** [Estimated Effort in Points]
**Notes:**
- [Additional information or edge cases]

---

# Admin User Stories

## Admin login

**Title:**
*As an admin, I want to log into the portal with my username and password, so that I can manage the platform securely.*

**Acceptance Criteria:**
1. The admin can enter a valid username and password on the login page.
2. Valid credentials authenticate the admin and redirect them to the admin dashboard.
3. Invalid credentials display an error message and do not create an authenticated session.

**Priority:** High
**Story Points:** 5
**Notes:**
- Passwords must be stored securely, and protected pages must only be accessible to authenticated admins.

## Admin logout

**Title:**
*As an admin, I want to log out of the portal, so that I can protect the system from unauthorized access.*

**Acceptance Criteria:**
1. The authenticated admin can select a logout option from the portal.
2. Logging out invalidates the current session and redirects the admin to the login page.
3. Protected pages cannot be accessed after logout without signing in again.

**Priority:** High
**Story Points:** 2
**Notes:**
- The browser back button must not restore access to authenticated functionality after logout.

## Add doctors

**Title:**
*As an admin, I want to add doctors to the portal, so that their profiles can be managed and used throughout the platform.*

**Acceptance Criteria:**
1. The admin can open a form and enter the required doctor information.
2. The system validates the submitted information and displays helpful messages for invalid or missing fields.
3. A valid doctor profile is saved and displayed in the portal's doctor list.

**Priority:** High
**Story Points:** 5
**Notes:**
- Duplicate doctor accounts or identifiers should be prevented.

## Delete a doctor's profile

**Title:**
*As an admin, I want to delete a doctor's profile from the portal, so that inactive or incorrectly created profiles are removed.*

**Acceptance Criteria:**
1. The admin can select an existing doctor profile for deletion.
2. The system asks the admin to confirm the operation before deleting the profile.
3. After confirmation, the profile is removed and no longer appears in the doctor list.

**Priority:** High
**Story Points:** 3
**Notes:**
- The system must safely handle doctors who have existing appointments or other related records.

## Monthly appointment statistics

**Title:**
*As an admin, I want to run a stored procedure in the MySQL CLI to get the number of appointments per month, so that I can track platform usage statistics.*

**Acceptance Criteria:**
1. A stored procedure is available and can be executed from the MySQL CLI.
2. The procedure returns appointment totals grouped by month in chronological order.
3. The returned totals accurately match the appointment records stored in MySQL.

**Priority:** Medium
**Story Points:** 5
**Notes:**
- The procedure should define how months with no appointments and records from different years are represented.

---

# Patient User Stories

## View available doctors

**Title:**
*As a patient, I want to view a list of doctors without logging in, so that I can explore my options before registering.*

**Acceptance Criteria:**
1. Any visitor can access the doctor list without being authenticated.
2. The list displays relevant information about each doctor, such as name, specialty, and availability.
3. Selecting a doctor displays additional public information about their profile.

**Priority:** High
**Story Points:** 3
**Notes:**
- Private doctor information must not be exposed on the public page.

## Patient registration

**Title:**
*As a patient, I want to sign up using my email and password, so that I can book appointments.*

**Acceptance Criteria:**
1. The patient can submit a valid email address and password through the registration form.
2. The system validates the information and prevents registration with an email address that is already in use.
3. A valid registration creates the patient account and allows the patient to proceed to the portal.

**Priority:** High
**Story Points:** 5
**Notes:**
- Passwords must meet the platform's security requirements and be stored securely.

## Patient login

**Title:**
*As a patient, I want to log into the portal, so that I can manage my bookings.*

**Acceptance Criteria:**
1. The patient can enter their registered email address and password on the login page.
2. Valid credentials authenticate the patient and redirect them to the patient dashboard.
3. Invalid credentials display an error message and do not create an authenticated session.

**Priority:** High
**Story Points:** 3
**Notes:**
- Only authenticated patients may access personal information and booking management features.

## Patient logout

**Title:**
*As a patient, I want to log out of the portal, so that I can secure my account.*

**Acceptance Criteria:**
1. The authenticated patient can select a logout option from the portal.
2. Logging out invalidates the current session and redirects the patient to a public page or the login page.
3. Protected patient pages cannot be accessed after logout without signing in again.

**Priority:** High
**Story Points:** 2
**Notes:**
- The browser back button must not restore access to authenticated functionality after logout.

## Book an hour-long appointment

**Title:**
*As a patient, I want to log in and book an hour-long appointment, so that I can consult with a doctor.*

**Acceptance Criteria:**
1. The authenticated patient can select a doctor and an available appointment date and start time.
2. Every appointment reserves a one-hour time slot and cannot overlap with another appointment for the selected doctor.
3. A successful booking is saved and a confirmation is displayed to the patient.

**Priority:** High
**Story Points:** 8
**Notes:**
- The system should account for the doctor's availability, working hours, and applicable time zone.

## View upcoming appointments

**Title:**
*As a patient, I want to view my upcoming appointments, so that I can prepare accordingly.*

**Acceptance Criteria:**
1. The authenticated patient can access a list of their future appointments.
2. Each appointment displays the doctor, date, start time, and relevant status.
3. Appointments are ordered chronologically, with the nearest appointment shown first.

**Priority:** High
**Story Points:** 3
**Notes:**
- Past or cancelled appointments should not appear as upcoming appointments.

---

# Doctor User Stories

## Doctor login

**Title:**
*As a doctor, I want to log into the portal, so that I can manage my appointments.*

**Acceptance Criteria:**
1. The doctor can enter valid credentials on the login page.
2. Valid credentials authenticate the doctor and redirect them to the doctor dashboard.
3. Invalid credentials display an error message and do not create an authenticated session.

**Priority:** High
**Story Points:** 3
**Notes:**
- Only authenticated doctors may access appointment and patient information.

## Doctor logout

**Title:**
*As a doctor, I want to log out of the portal, so that I can protect my data.*

**Acceptance Criteria:**
1. The authenticated doctor can select a logout option from the portal.
2. Logging out invalidates the current session and redirects the doctor to the login page.
3. Protected doctor pages cannot be accessed after logout without signing in again.

**Priority:** High
**Story Points:** 2
**Notes:**
- The browser back button must not restore access to authenticated functionality after logout.

## View appointment calendar

**Title:**
*As a doctor, I want to view my appointment calendar, so that I can stay organized.*

**Acceptance Criteria:**
1. The authenticated doctor can access a calendar containing their scheduled appointments.
2. Each appointment displays its date, time, patient, and current status.
3. The doctor can navigate between relevant calendar periods, such as days, weeks, or months.

**Priority:** High
**Story Points:** 5
**Notes:**
- Appointment times should be displayed consistently in the doctor's applicable time zone.

## Mark unavailability

**Title:**
*As a doctor, I want to mark when I am unavailable, so that patients are only shown appointment slots when I am available.*

**Acceptance Criteria:**
1. The doctor can select a date and time range and mark it as unavailable.
2. Unavailable periods are excluded from the appointment slots displayed to patients.
3. The system prevents unavailability from being added when it conflicts with an existing confirmed appointment.

**Priority:** High
**Story Points:** 8
**Notes:**
- The doctor should be able to update or remove future unavailability periods.

## Update doctor profile

**Title:**
*As a doctor, I want to update my profile with my specialization and contact information, so that patients have up-to-date information.*

**Acceptance Criteria:**
1. The authenticated doctor can edit their specialization and permitted contact information.
2. The system validates required fields and displays helpful messages for invalid values.
3. Valid changes are saved and displayed on the doctor's current profile.

**Priority:** Medium
**Story Points:** 3
**Notes:**
- Sensitive or administrative profile fields must not be editable through this feature.

## View patient details for upcoming appointments

**Title:**
*As a doctor, I want to view patient details for my upcoming appointments, so that I can be prepared.*

**Acceptance Criteria:**
1. The authenticated doctor can select one of their upcoming appointments and view the associated patient details.
2. The page displays only the patient information relevant to preparing for the consultation.
3. A doctor cannot access patient details through appointments assigned to another doctor.

**Priority:** High
**Story Points:** 5
**Notes:**
- Patient information must be handled according to applicable privacy and access-control requirements.
