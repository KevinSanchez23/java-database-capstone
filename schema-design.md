# Database Schema Design

## MySQL Database Design

MySQL stores the structured, relational data for patients, doctors, administrators, appointments, and doctor availability. All tables use the `InnoDB` storage engine so that foreign keys and transactions are supported.

### Table: `patients`

- `id`: `BIGINT UNSIGNED`, Primary Key, Auto Increment
- `name`: `VARCHAR(100)`, Not Null
- `email`: `VARCHAR(254)`, Not Null, Unique
- `password_hash`: `VARCHAR(255)`, Not Null
- `phone`: `VARCHAR(20)`, Not Null
- `address`: `VARCHAR(255)`, Not Null
- `active`: `BOOLEAN`, Not Null, Default `TRUE`
- `created_at`: `TIMESTAMP`, Not Null, Default `CURRENT_TIMESTAMP`
- `updated_at`: `TIMESTAMP`, Not Null, Default `CURRENT_TIMESTAMP`, updated automatically

**Design notes:**

- Passwords must be stored as secure hashes, never as plain text.
- Email format, phone format, password strength, and name length should be validated in Java using Bean Validation. The database still enforces required values and unique email addresses.
- Patients should normally be deactivated by setting `active` to `FALSE` instead of being physically deleted. This preserves their appointment history.

### Table: `doctors`

- `id`: `BIGINT UNSIGNED`, Primary Key, Auto Increment
- `name`: `VARCHAR(100)`, Not Null
- `specialty`: `VARCHAR(50)`, Not Null
- `email`: `VARCHAR(254)`, Not Null, Unique
- `password_hash`: `VARCHAR(255)`, Not Null
- `phone`: `VARCHAR(20)`, Not Null
- `active`: `BOOLEAN`, Not Null, Default `TRUE`
- `created_at`: `TIMESTAMP`, Not Null, Default `CURRENT_TIMESTAMP`
- `updated_at`: `TIMESTAMP`, Not Null, Default `CURRENT_TIMESTAMP`, updated automatically

**Design notes:**

- Email and phone formats should be validated in Java. MySQL should enforce uniqueness of the email address.
- Doctors with appointment history should be deactivated rather than deleted. This prevents historical appointments and prescriptions from losing their medical context.
- Only active doctors should be displayed to patients or allowed to receive new appointments.

### Table: `admins`

- `id`: `BIGINT UNSIGNED`, Primary Key, Auto Increment
- `username`: `VARCHAR(50)`, Not Null, Unique
- `password_hash`: `VARCHAR(255)`, Not Null
- `active`: `BOOLEAN`, Not Null, Default `TRUE`
- `created_at`: `TIMESTAMP`, Not Null, Default `CURRENT_TIMESTAMP`
- `updated_at`: `TIMESTAMP`, Not Null, Default `CURRENT_TIMESTAMP`, updated automatically

**Design notes:**

- Usernames should be treated consistently with respect to case, and duplicate usernames must not be allowed.
- Admin passwords must be hashed. Password strength and login rules should be enforced in the application.

### Table: `doctor_availability`

- `id`: `BIGINT UNSIGNED`, Primary Key, Auto Increment
- `doctor_id`: `BIGINT UNSIGNED`, Not Null, Foreign Key → `doctors(id)`
- `start_time`: `DATETIME`, Not Null
- `end_time`: `DATETIME`, Not Null
- `status`: `TINYINT`, Not Null, Default `0` (`0` = Available, `1` = Unavailable)
- `created_at`: `TIMESTAMP`, Not Null, Default `CURRENT_TIMESTAMP`
- Unique constraint: (`doctor_id`, `start_time`)
- Check constraint: `end_time > start_time`
- Foreign key behavior: `ON DELETE RESTRICT`, `ON UPDATE CASCADE`

**Design notes:**

- Each doctor has individual time slots. For the current requirements, bookable slots should last exactly one hour.
- A doctor can mark a future slot as unavailable, after which it must not be offered to patients.
- The service layer must reject overlapping availability ranges. The unique constraint prevents duplicate start times, while transactional application validation handles other kinds of overlap.
- Existing slots connected to appointments must not be deleted. Their status can be changed when appropriate so that history remains intact.

### Table: `appointments`

- `id`: `BIGINT UNSIGNED`, Primary Key, Auto Increment
- `doctor_id`: `BIGINT UNSIGNED`, Not Null, Foreign Key → `doctors(id)`
- `patient_id`: `BIGINT UNSIGNED`, Not Null, Foreign Key → `patients(id)`
- `availability_id`: `BIGINT UNSIGNED`, Not Null, Foreign Key → `doctor_availability(id)`, Unique
- `appointment_time`: `DATETIME`, Not Null
- `duration_minutes`: `SMALLINT UNSIGNED`, Not Null, Default `60`
- `status`: `TINYINT`, Not Null, Default `0` (`0` = Scheduled, `1` = Completed, `2` = Cancelled)
- `created_at`: `TIMESTAMP`, Not Null, Default `CURRENT_TIMESTAMP`
- `updated_at`: `TIMESTAMP`, Not Null, Default `CURRENT_TIMESTAMP`, updated automatically
- Unique constraint: (`doctor_id`, `appointment_time`)
- Check constraint: `duration_minutes = 60`
- Check constraint: `status IN (0, 1, 2)`
- Foreign key behavior: patient, doctor, and availability references use `ON DELETE RESTRICT` and `ON UPDATE CASCADE`

**Design notes:**

- A unique `availability_id` guarantees that an available slot can be assigned to only one appointment.
- The (`doctor_id`, `appointment_time`) unique constraint prevents two appointments from starting at the same time for the same doctor. Booking must run inside a transaction to avoid race conditions.
- The service layer must also verify that the selected availability slot belongs to the same doctor, starts at `appointment_time`, is available, and does not overlap another active appointment.
- Appointments should not be physically deleted when a patient or doctor leaves the system. `ON DELETE RESTRICT` plus logical deactivation preserves the medical and usage history.
- Cancelled appointments remain stored with status `2`. This supports auditing and accurate monthly usage statistics.

### Relationships

- One patient can have many appointments: `patients (1) → (N) appointments`.
- One doctor can have many appointments: `doctors (1) → (N) appointments`.
- One doctor can define many availability slots: `doctors (1) → (N) doctor_availability`.
- One availability slot can be associated with zero or one appointment.

### Recommended indexes

- Index `appointments(patient_id, appointment_time)` to retrieve a patient's upcoming appointments efficiently.
- Index `appointments(doctor_id, appointment_time)` to build the doctor's calendar and enforce unique start times.
- Index `appointments(status, appointment_time)` for operational and monthly statistics.
- Index `doctor_availability(doctor_id, status, start_time)` to find bookable slots.
- Unique indexes on `patients.email`, `doctors.email`, and `admins.username`.

### Integrity and lifecycle decisions

- Patient and doctor records use logical deactivation instead of cascading deletion. Past appointments should be retained for history, reporting, and prescription references.
- Doctors must not have overlapping appointments. The database prevents duplicate slots, while the service layer performs an overlap check inside the same booking transaction.
- Appointment dates must be in the future when created. This rule is time-dependent and should be validated in Java rather than with a MySQL `CHECK` constraint.
- Email and phone syntax should be validated in Java; database constraints focus on length, presence, and uniqueness.
- Prescriptions are stored in MongoDB but must contain the related MySQL `appointment_id`. A prescription should not exist independently because tying it to an appointment identifies both the patient and the prescribing doctor.
- The application must verify that a referenced appointment exists before creating a MongoDB prescription, because MySQL cannot enforce a foreign key against MongoDB.

## MongoDB Collection Design

MongoDB complements the relational schema by storing prescription documents. Prescriptions may contain a variable number of medications, free-form medical instructions, pharmacy information, attachments, and metadata that can evolve without requiring frequent relational schema migrations.

### Collection: `prescriptions`

```json
{
  "_id": "64abc1234567890abcdef1234",
  "schemaVersion": 1,
  "appointmentId": 51,
  "patientId": 18,
  "doctorId": 7,
  "patientSnapshot": {
    "name": "John Smith"
  },
  "doctorSnapshot": {
    "name": "Dr. Ana Lopez",
    "specialty": "General Medicine"
  },
  "medications": [
    {
      "name": "Paracetamol",
      "dosage": "500 mg",
      "frequency": "Every 6 hours",
      "durationDays": 5,
      "route": "Oral",
      "refillCount": 2,
      "instructions": "Take after food."
    }
  ],
  "doctorNotes": "Contact the clinic if the fever lasts more than three days.",
  "diagnosisTags": [
    "fever",
    "pain-management"
  ],
  "pharmacy": {
    "name": "Central Pharmacy",
    "address": {
      "street": "100 Market Street",
      "city": "Mexico City",
      "postalCode": "06000"
    },
    "phone": "+52-55-5555-0100"
  },
  "attachments": [
    {
      "fileName": "lab-results.pdf",
      "contentType": "application/pdf",
      "storageKey": "prescriptions/51/lab-results.pdf",
      "uploadedAt": "2026-07-20T16:30:00Z"
    }
  ],
  "metadata": {
    "source": "doctor-portal",
    "language": "en",
    "lastModifiedBy": "doctor:7"
  },
  "status": "ACTIVE",
  "createdAt": "2026-07-20T16:20:00Z",
  "updatedAt": "2026-07-20T16:30:00Z"
}
```

### Field and relationship decisions

- `_id` is generated by MongoDB as an `ObjectId`. It is represented as a string in the JSON example so that the example remains valid JSON.
- `appointmentId`, `patientId`, and `doctorId` reference records stored in MySQL. MongoDB cannot enforce these foreign keys, so the Spring service must verify them before saving a prescription.
- The complete patient and doctor objects should not be duplicated. IDs remain the source of truth and avoid storing unnecessary personal data.
- Small `patientSnapshot` and `doctorSnapshot` objects preserve the names and specialty shown when the prescription was created. This historical snapshot remains stable if a profile changes later.
- `medications` is an array because one prescription may contain multiple medicines. Each medicine is embedded because its dosage and instructions belong to this prescription.
- `doctorNotes` supports free-form text, while `diagnosisTags` enables simple classification and searching.
- `pharmacy` is optional and embedded because its information is specific to the prescription.
- `attachments` stores metadata and a storage reference, not the binary file itself. The actual file should be stored in object storage; GridFS can be considered if files must remain inside MongoDB.
- `status` should use controlled values such as `ACTIVE`, `CANCELLED`, or `REPLACED`. Prescriptions should normally be retained instead of physically deleted.

### Recommended indexes

- Unique index on `appointmentId` if the application allows only one prescription document per appointment.
- Index on `patientId` and `createdAt` to retrieve a patient's prescription history.
- Index on `doctorId` and `createdAt` to retrieve prescriptions issued by a doctor.
- Multikey index on `diagnosisTags` if prescriptions will be searched or filtered by tags.

### Schema evolution

- `schemaVersion` identifies the document structure and lets the application migrate or interpret older prescriptions when new fields are introduced.
- New optional fields and embedded objects can be added without rewriting every existing document.
- Required fields should still be validated with Spring Data validation and, where appropriate, a MongoDB JSON Schema validator.
- The application must provide defaults or tolerate missing fields when reading older document versions.

### Future messaging design

Chat messages should use a separate `messages` collection rather than being embedded in prescriptions. A message would reference `appointmentId`, `senderId`, and `recipientId`, and could contain message text, attachment metadata, timestamps, and read status. Keeping messages separate prevents prescription documents from growing without limit and allows messages to be queried and paginated efficiently.
