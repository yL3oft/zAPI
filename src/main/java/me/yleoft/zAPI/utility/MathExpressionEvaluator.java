package me.yleoft.zAPI.utility;

import org.jetbrains.annotations.NotNull;

import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for evaluating mathematical expressions in display conditions.
 * Supports basic arithmetic operations and mathematical functions.
 */
public class MathExpressionEvaluator {

    /**
     * Evaluates a mathematical expression and returns the result.
     *
     * Supported operations:
     * - Basic: +, -, *, /
     * - Functions: sqrt(), round(), roundDown()
     *
     * @param expression The mathematical expression to evaluate
     * @return The result of the evaluation
     * @throws IllegalArgumentException if the expression is invalid
     */
    public static double evaluate(@NotNull String expression) throws IllegalArgumentException {
        try {
            // Remove all whitespace
            String cleaned = expression.replaceAll("\\s+", "");

            // Process functions first (sqrt, round, roundDown)
            cleaned = processFunctions(cleaned);

            // Evaluate the expression using shunting yard algorithm
            return evaluateExpression(cleaned);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid mathematical expression: " + expression, e);
        }
    }

    /**
     * Process mathematical functions in the expression.
     */
    private static String processFunctions(@NotNull String expression) {
        String result = expression;

        // Process roundDown() function
        result = processFunction(result, "roundDown", MathExpressionEvaluator::roundDown);

        // Process round() function
        result = processFunction(result, "round", MathExpressionEvaluator::round);

        // Process sqrt() function
        result = processFunction(result, "sqrt", Math::sqrt);

        return result;
    }

    /**
     * Process a specific function in the expression.
     */
    private static String processFunction(@NotNull String expression,
                                          @NotNull String functionName,
                                          @NotNull MathFunction function) {
        // Pattern to match function calls: functionName(...)
        Pattern pattern = Pattern.compile(functionName + "\\(([^()]+)\\)");
        Matcher matcher = pattern.matcher(expression);

        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String innerExpression = matcher.group(1);
            // Recursively evaluate the inner expression
            double innerValue = evaluate(innerExpression);
            double functionResult = function.apply(innerValue);
            matcher.appendReplacement(result, String.valueOf(functionResult));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Evaluates a mathematical expression using the Shunting Yard algorithm.
     */
    private static double evaluateExpression(@NotNull String expression) {
        Stack<Double> values = new Stack<>();
        Stack<Character> operators = new Stack<>();

        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);

            // Skip whitespace (though it should already be removed)
            if (Character.isWhitespace(c)) {
                continue;
            }

            // Handle numbers (including decimals and negative numbers)
            if (Character.isDigit(c) || c == '.') {
                StringBuilder number = new StringBuilder();
                while (i < expression.length() &&
                        (Character.isDigit(expression.charAt(i)) || expression.charAt(i) == '.')) {
                    number.append(expression.charAt(i));
                    i++;
                }
                i--; // Step back one position
                values.push(Double.parseDouble(number.toString()));
            }
            // Handle opening parenthesis
            else if (c == '(') {
                operators.push(c);
            }
            // Handle closing parenthesis
            else if (c == ')') {
                while (!operators.isEmpty() && operators.peek() != '(') {
                    values.push(applyOperator(operators.pop(), values.pop(), values.pop()));
                }
                if (!operators.isEmpty()) {
                    operators.pop(); // Remove '('
                }
            }
            // Handle operators
            else if (isOperator(c)) {
                // Handle negative numbers (unary minus)
                if (c == '-' && (i == 0 || expression.charAt(i - 1) == '(' || isOperator(expression.charAt(i - 1)))) {
                    // This is a negative number
                    StringBuilder number = new StringBuilder("-");
                    i++;
                    while (i < expression.length() &&
                            (Character.isDigit(expression.charAt(i)) || expression.charAt(i) == '.')) {
                        number.append(expression.charAt(i));
                        i++;
                    }
                    i--; // Step back one position
                    values.push(Double.parseDouble(number.toString()));
                } else {
                    // Process operators with precedence
                    while (!operators.isEmpty() && precedence(c) <= precedence(operators.peek())) {
                        values.push(applyOperator(operators.pop(), values.pop(), values.pop()));
                    }
                    operators.push(c);
                }
            }
        }

        // Apply remaining operators
        while (!operators.isEmpty()) {
            values.push(applyOperator(operators.pop(), values.pop(), values.pop()));
        }

        return values.isEmpty() ? 0.0 : values.pop();
    }

    /**
     * Checks if a character is an operator.
     */
    private static boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/';
    }

    /**
     * Returns the precedence of an operator.
     */
    private static int precedence(char operator) {
        switch (operator) {
            case '+':
            case '-':
                return 1;
            case '*':
            case '/':
                return 2;
            default:
                return 0;
        }
    }

    /**
     * Applies an operator to two operands.
     */
    private static double applyOperator(char operator, double b, double a) {
        switch (operator) {
            case '+':
                return a + b;
            case '-':
                return a - b;
            case '*':
                return a * b;
            case '/':
                if (b == 0) {
                    throw new ArithmeticException("Division by zero");
                }
                return a / b;
            default:
                throw new IllegalArgumentException("Unknown operator: " + operator);
        }
    }

    /**
     * Rounds a number using standard rounding rules (0.5 rounds up).
     */
    private static double round(double value) {
        return Math.round(value);
    }

    /**
     * Rounds a number down (floor function).
     */
    private static double roundDown(double value) {
        return Math.floor(value);
    }

    /**
     * Functional interface for mathematical functions.
     */
    @FunctionalInterface
    private interface MathFunction {
        double apply(double value);
    }
}