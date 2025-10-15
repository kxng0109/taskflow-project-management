package io.github.kxng0109.taskflow.task.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TaskStatusValidator.class)
@Documented
public @interface ValidTaskStatus {
    String message() default "Invalid status. Must be one of: TO_DO, IN_PROGRESS, DONE";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}