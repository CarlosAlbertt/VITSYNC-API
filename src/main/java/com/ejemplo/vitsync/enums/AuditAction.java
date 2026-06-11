package com.ejemplo.vitsync.enums;

/**
 * Auditable actions recorded in {@code audit_logs} to satisfy the GDPR
 * record-of-processing obligation (Art. 30) and the patient's right to know
 * who accessed their clinical record (Ley 41/2002 art. 16.7, LOPDGDD DA 17ª).
 *
 * @author VitSync Team
 * @version 1.0
 * @since 2.0
 */
public enum AuditAction {
    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    LOGOUT,
    REGISTER,
    PASSWORD_CHANGE,
    VIEW_PATIENT_DATA,
    VIEW_MEDICAL_REPORT,
    CREATE_APPOINTMENT,
    MODIFY_APPOINTMENT,
    CANCEL_APPOINTMENT,
    VIEW_CHAT,
    EXPORT_DATA,
    ADMIN_ACCESS,
    DATA_DELETION_REQUEST
}
