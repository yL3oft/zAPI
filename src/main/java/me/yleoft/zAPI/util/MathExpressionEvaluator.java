package me.yleoft.zAPI.util;

import org.jetbrains.annotations.NotNull;

import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MathExpressionEvaluator {

    public static double evaluate(@NotNull String expression) throws IllegalArgumentException {
        try {
            String cleaned = expression.replaceAll("\\s+", "");
            cleaned = processFunctions(cleaned);
            return evaluateExpression(cleaned);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid mathematical expression: " + expression, e);
        }
    }

    /**
     * Evaluates a string as a number, using the math evaluator if it contains operators.
     * Returns 0.0 if the string cannot be parsed.
     */
    public static double evaluateNumeric(@NotNull String expression) {
        try {
            if (containsMathOperators(expression)) {
                return evaluate(expression);
            }
            return Double.parseDouble(expression.trim());
        } catch (Exception e) {
            try {
                return Double.parseDouble(expression.trim());
            } catch (NumberFormatException nfe) {
                return 0.0;
            }
        }
    }

    /**
     * Evaluates a string as a math expression if it contains operators,
     * otherwise returns it as-is (as a string). Used for equality comparisons.
     */
    @NotNull
    public static String evaluateMathIfNeeded(@NotNull String expression) {
        if (containsMathOperators(expression)) {
            try {
                double result = evaluate(expression);
                if (result == Math.floor(result)) return String.valueOf((long) result);
                return String.valueOf(result);
            } catch (Exception e) {
                return expression;
            }
        }
        return expression;
    }

    /**
     * Returns true if the expression contains arithmetic operators or math functions.
     */
    public static boolean containsMathOperators(@NotNull String expression) {
        String trimmed = expression.trim();
        if (trimmed.contains("sqrt(") || trimmed.contains("round(") || trimmed.contains("roundDown(")) {
            return true;
        }
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c == '*' || c == '/') return true;
            if ((c == '+' || c == '-') && i > 0) return true;
        }
        return false;
    }

    private static String processFunctions(@NotNull String expression) {
        String result = expression;
        result = processFunction(result, "roundDown", MathExpressionEvaluator::roundDown);
        result = processFunction(result, "round",     MathExpressionEvaluator::round);
        result = processFunction(result, "sqrt",      Math::sqrt);
        return result;
    }

    private static String processFunction(@NotNull String expression, @NotNull String functionName,
                                          @NotNull MathFunction function) {
        Pattern pattern = Pattern.compile(functionName + "\\(([^()]+)\\)");
        Matcher matcher = pattern.matcher(expression);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            double innerValue = evaluate(matcher.group(1));
            matcher.appendReplacement(result, String.valueOf(function.apply(innerValue)));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static double evaluateExpression(@NotNull String expression) {
        Stack<Double> values = new Stack<>();
        Stack<Character> operators = new Stack<>();

        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (Character.isWhitespace(c)) continue;

            if (Character.isDigit(c) || c == '.') {
                StringBuilder number = new StringBuilder();
                while (i < expression.length() &&
                        (Character.isDigit(expression.charAt(i)) || expression.charAt(i) == '.')) {
                    number.append(expression.charAt(i++));
                }
                i--;
                values.push(Double.parseDouble(number.toString()));
            } else if (c == '(') {
                operators.push(c);
            } else if (c == ')') {
                while (!operators.isEmpty() && operators.peek() != '(') {
                    values.push(applyOperator(operators.pop(), values.pop(), values.pop()));
                }
                if (!operators.isEmpty()) operators.pop();
            } else if (isOperator(c)) {
                if (c == '-' && (i == 0 || expression.charAt(i - 1) == '(' || isOperator(expression.charAt(i - 1)))) {
                    StringBuilder number = new StringBuilder("-");
                    i++;
                    while (i < expression.length() &&
                            (Character.isDigit(expression.charAt(i)) || expression.charAt(i) == '.')) {
                        number.append(expression.charAt(i++));
                    }
                    i--;
                    values.push(Double.parseDouble(number.toString()));
                } else {
                    while (!operators.isEmpty() && precedence(c) <= precedence(operators.peek())) {
                        values.push(applyOperator(operators.pop(), values.pop(), values.pop()));
                    }
                    operators.push(c);
                }
            }
        }

        while (!operators.isEmpty()) {
            values.push(applyOperator(operators.pop(), values.pop(), values.pop()));
        }
        return values.isEmpty() ? 0.0 : values.pop();
    }

    private static boolean isOperator(char c) { return c == '+' || c == '-' || c == '*' || c == '/'; }

    private static int precedence(char op) {
        return switch (op) {
            case '+', '-' -> 1;
            case '*', '/' -> 2;
            default -> 0;
        };
    }

    private static double applyOperator(char op, double b, double a) {
        return switch (op) {
            case '+' -> a + b;
            case '-' -> a - b;
            case '*' -> a * b;
            case '/' -> { if (b == 0) throw new ArithmeticException("Division by zero"); yield a / b; }
            default  -> throw new IllegalArgumentException("Unknown operator: " + op);
        };
    }

    private static double round(double value) { return Math.round(value); }
    private static double roundDown(double value) { return Math.floor(value); }

    @FunctionalInterface
    private interface MathFunction { double apply(double value); }
}