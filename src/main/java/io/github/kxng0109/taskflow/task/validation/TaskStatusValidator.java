package io.github.kxng0109.taskflow.task.validation;

import io.github.kxng0109.taskflow.task.TaskStatus;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;

public class TaskStatusValidator implements ConstraintValidator<ValidTaskStatus, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if(value == null) return false;

        return Arrays.stream(TaskStatus.values())
                .anyMatch(taskStatus -> taskStatus.name().equalsIgnoreCase(value));
    }
}
