package com.ejemplo.vitsync.audit;

import com.ejemplo.vitsync.enums.AuditAction;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a service method whose invocation must be recorded in the audit trail.
 *
 * <p>Intercepted by {@link AuditAspect}: a successful return logs the action
 * with {@code success = true}; a thrown exception logs it with
 * {@code success = false} and rethrows. Apply it to service methods that
 * access or mutate clinical/personal data (GDPR Art. 30).</p>
 *
 * <p>Example:
 * {@code @Auditable(action = AuditAction.VIEW_MEDICAL_REPORT, targetIdIndex = 0)}</p>
 *
 * @author VitSync Team
 * @version 1.0
 * @since 2.0
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    /** @return the action to record */
    AuditAction action();

    /**
     * @return index of the method argument to record as {@code targetId}, or
     *         {@code -1} (default) to record no target
     */
    int targetIdIndex() default -1;
}
