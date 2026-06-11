package com.ejemplo.vitsync.audit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * AOP aspect that records {@link Auditable} service-method invocations.
 *
 * <p>Wraps each annotated method: on normal return it logs the action with
 * {@code success = true}; if the method throws, it logs {@code success = false}
 * with the exception type as detail and rethrows the original exception
 * (auditing must never swallow business errors).</p>
 *
 * @author VitSync Team
 * @version 1.0
 * @since 2.0
 */
@Aspect
@Component
public class AuditAspect {

    private final AuditService auditService;

    public AuditAspect(AuditService auditService) {
        this.auditService = auditService;
    }

    /** Matches any method annotated with {@link Auditable}. */
    @Pointcut("@annotation(auditable)")
    public void auditableMethod(Auditable auditable) {
    }

    /**
     * Around advice that brackets the method with audit logging.
     *
     * @param joinPoint the intercepted invocation
     * @param auditable the annotation instance (action + target index)
     * @return whatever the method returns
     * @throws Throwable rethrows any exception from the method, after auditing
     */
    @Around(value = "auditableMethod(auditable)", argNames = "joinPoint,auditable")
    public Object audit(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        String targetId = resolveTargetId(joinPoint, auditable.targetIdIndex());
        try {
            Object result = joinPoint.proceed();
            auditService.record(auditable.action(), targetId, true, null);
            return result;
        } catch (Throwable ex) {
            // Registramos el fallo (sin datos sensibles: solo el tipo) y relanzamos
            auditService.record(auditable.action(), targetId, false,
                    ex.getClass().getSimpleName());
            throw ex;
        }
    }

    /** Reads the configured argument as the target id, defensively. */
    private String resolveTargetId(ProceedingJoinPoint joinPoint, int index) {
        if (index < 0) {
            return null;
        }
        Object[] args = joinPoint.getArgs();
        if (index >= args.length || args[index] == null) {
            return null;
        }
        return String.valueOf(args[index]);
    }
}
