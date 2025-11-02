package kexamprint.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds validation results including errors and warnings
 */
public class ValidationResult {
    private final List<ValidationIssue> errors;
    private final List<ValidationIssue> warnings;

    public ValidationResult() {
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
    }

    public void addError(String category, String message, Object... context) {
        errors.add(new ValidationIssue(IssueType.ERROR, category, message, context));
    }

    public void addWarning(String category, String message, Object... context) {
        warnings.add(new ValidationIssue(IssueType.WARNING, category, message, context));
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    public boolean isValid() {
        return !hasErrors();
    }

    public List<ValidationIssue> getErrors() {
        return new ArrayList<>(errors);
    }

    public List<ValidationIssue> getWarnings() {
        return new ArrayList<>(warnings);
    }

    public int getErrorCount() {
        return errors.size();
    }

    public int getWarningCount() {
        return warnings.size();
    }

    /**
     * Represents a single validation issue
     */
    public static class ValidationIssue {
        private final IssueType type;
        private final String category;
        private final String message;
        private final Object[] context;

        public ValidationIssue(IssueType type, String category, String message, Object... context) {
            this.type = type;
            this.category = category;
            this.message = message;
            this.context = context;
        }

        public IssueType getType() {
            return type;
        }

        public String getCategory() {
            return category;
        }

        public String getMessage() {
            return message;
        }

        public Object[] getContext() {
            return context;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[").append(type).append("] ");
            sb.append(category).append(": ");
            sb.append(message);
            if (context != null && context.length > 0) {
                sb.append(" (");
                for (int i = 0; i < context.length; i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(context[i]);
                }
                sb.append(")");
            }
            return sb.toString();
        }
    }

    public enum IssueType {
        ERROR,
        WARNING
    }
}
