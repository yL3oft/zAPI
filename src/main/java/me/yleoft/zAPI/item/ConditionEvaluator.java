package me.yleoft.zAPI.item;

import me.yleoft.zAPI.util.MathExpressionEvaluator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Evaluates display conditions for inventory items.
 * Supports comparison operators (==, !=, >=, <=, >, <) and mathematical expressions.
 */
public final class ConditionEvaluator {

    private ConditionEvaluator() {}

    /**
     * Evaluates a pre-processed condition string (all placeholders already applied).
     * Supports: ==, !=, >=, <=, >, < with math expressions on either side.
     *
     * @param processed The condition string with all placeholders already substituted
     * @return true if the condition passes
     */
    public static boolean evaluate(@NotNull String processed) {
        int notEqualsIndex = findOperatorIndex(processed, "!=");
        if (notEqualsIndex != -1) {
            String left  = processed.substring(0, notEqualsIndex).trim();
            String right = processed.substring(notEqualsIndex + 2).trim();
            return !MathExpressionEvaluator.evaluateMathIfNeeded(left)
                    .equals(MathExpressionEvaluator.evaluateMathIfNeeded(right));
        }

        int equalsIndex = findOperatorIndex(processed, "==");
        if (equalsIndex != -1) {
            String left  = processed.substring(0, equalsIndex).trim();
            String right = processed.substring(equalsIndex + 2).trim();
            return MathExpressionEvaluator.evaluateMathIfNeeded(left)
                    .equals(MathExpressionEvaluator.evaluateMathIfNeeded(right));
        }

        int greaterEqualsIndex = findOperatorIndex(processed, ">=");
        if (greaterEqualsIndex != -1) {
            double left  = MathExpressionEvaluator.evaluateNumeric(processed.substring(0, greaterEqualsIndex).trim());
            double right = MathExpressionEvaluator.evaluateNumeric(processed.substring(greaterEqualsIndex + 2).trim());
            return left >= right;
        }

        int lessEqualsIndex = findOperatorIndex(processed, "<=");
        if (lessEqualsIndex != -1) {
            double left  = MathExpressionEvaluator.evaluateNumeric(processed.substring(0, lessEqualsIndex).trim());
            double right = MathExpressionEvaluator.evaluateNumeric(processed.substring(lessEqualsIndex + 2).trim());
            return left <= right;
        }

        int greaterIndex = findOperatorIndex(processed, ">");
        if (greaterIndex != -1 && !isPartOfTwoCharOperator(processed, greaterIndex)) {
            double left  = MathExpressionEvaluator.evaluateNumeric(processed.substring(0, greaterIndex).trim());
            double right = MathExpressionEvaluator.evaluateNumeric(processed.substring(greaterIndex + 1).trim());
            return left > right;
        }

        int lessIndex = findOperatorIndex(processed, "<");
        if (lessIndex != -1 && !isPartOfTwoCharOperator(processed, lessIndex)) {
            double left  = MathExpressionEvaluator.evaluateNumeric(processed.substring(0, lessIndex).trim());
            double right = MathExpressionEvaluator.evaluateNumeric(processed.substring(lessIndex + 1).trim());
            return left < right;
        }

        return false;
    }

    private static int findOperatorIndex(@NotNull String text, @NotNull String operator) {
        int index = text.indexOf(operator);
        while (index == 0 && text.length() > operator.length()) {
            index = text.indexOf(operator, index + 1);
        }
        return index > 0 ? index : -1;
    }

    private static boolean isPartOfTwoCharOperator(@NotNull String text, int index) {
        return index + 1 < text.length() && text.charAt(index + 1) == '=';
    }
}