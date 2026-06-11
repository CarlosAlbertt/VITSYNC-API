package com.ejemplo.vitsync.audit;

import com.ejemplo.vitsync.enums.AuditAction;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AuditAspect} using a mocked join point.
 */
@DisplayName("AuditAspect — automatic audit logging")
class AuditAspectTest {

    private AuditService auditService;
    private AuditAspect aspect;

    @BeforeEach
    void setUp() {
        auditService = mock(AuditService.class);
        aspect = new AuditAspect(auditService);
    }

    private Auditable annotation(AuditAction action, int targetIndex) {
        return new Auditable() {
            @Override public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return Auditable.class;
            }
            @Override public AuditAction action() { return action; }
            @Override public int targetIdIndex() { return targetIndex; }
        };
    }

    @Test
    @DisplayName("Successful method records success=true with the target id")
    void successfulMethod_recordsSuccess() throws Throwable {
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        when(pjp.getArgs()).thenReturn(new Object[]{42L});
        when(pjp.proceed()).thenReturn("ok");

        Object result = aspect.audit(pjp, annotation(AuditAction.VIEW_MEDICAL_REPORT, 0));

        assertEquals("ok", result);
        verify(auditService).record(AuditAction.VIEW_MEDICAL_REPORT, "42", true, null);
    }

    @Test
    @DisplayName("Throwing method records success=false and rethrows")
    void throwingMethod_recordsFailureAndRethrows() throws Throwable {
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        when(pjp.getArgs()).thenReturn(new Object[]{7L});
        when(pjp.proceed()).thenThrow(new IllegalArgumentException("boom"));

        assertThrows(IllegalArgumentException.class,
                () -> aspect.audit(pjp, annotation(AuditAction.CANCEL_APPOINTMENT, 0)));

        verify(auditService).record(eq(AuditAction.CANCEL_APPOINTMENT), eq("7"),
                eq(false), eq("IllegalArgumentException"));
    }

    @Test
    @DisplayName("targetIdIndex = -1 records a null target")
    void noTargetIndex_recordsNullTarget() throws Throwable {
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        when(pjp.proceed()).thenReturn(null);

        aspect.audit(pjp, annotation(AuditAction.EXPORT_DATA, -1));

        verify(auditService).record(AuditAction.EXPORT_DATA, null, true, null);
    }
}
