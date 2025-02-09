package iney.lol.equationapi.utils;

import java.util.*;

public class EquationParser {
    private final String equation;
    private int position;
    private final char[] chars;

    public EquationParser(String equation) {
        // Предварительная обработка уравнения
        this.equation = prepareEquation(equation);
        this.chars = this.equation.toCharArray();
        this.position = 0;
    }

    // Основной метод для вычисления значения
    public static double evaluate(String equation, double xValue) {
        EquationParser parser = new EquationParser(equation);
        return parser.calculate(xValue);
    }

    private double calculate(double xValue) {
        // Если есть знак равенства, преобразуем в разность частей
        if (equation.contains("=")) {
            String[] parts = equation.split("=");
            EquationParser leftPart = new EquationParser(parts[0]);
            EquationParser rightPart = new EquationParser(parts[1]);
            return leftPart.calculate(xValue) - rightPart.calculate(xValue);
        }

        return parseExpression(xValue);
    }

    private String prepareEquation(String eq) {
        // Удаляем пробелы
        eq = eq.replaceAll("\\s+", "");

        // Заменяем константы
        eq = eq.replaceAll("pi", String.valueOf(Math.PI));
        eq = eq.replaceAll("e", String.valueOf(Math.E));

        // Добавляем умножение перед функциями, если нужно
        eq = eq.replaceAll("(\\d)(sin|cos|tan|log|ln|sqrt)", "$1*$2");

        // Добавляем явное умножение для x
        eq = eq.replaceAll("(\\d)x", "$1*x");
        eq = eq.replaceAll("x(\\d)", "x*$1");

        return eq;
    }

    private double parseExpression(double xValue) {
        double result = parseTerm(xValue);

        while (position < chars.length) {
            char operator = chars[position];
            if (operator != '+' && operator != '-') break;

            position++;
            double value = parseTerm(xValue);

            if (operator == '+') {
                result += value;
            } else {
                result -= value;
            }
        }

        return result;
    }

    private double parseTerm(double xValue) {
        double result = parseFactor(xValue);

        while (position < chars.length) {
            char operator = chars[position];
            if (operator != '*' && operator != '/') break;

            position++;
            double value = parseFactor(xValue);

            if (operator == '*') {
                result *= value;
            } else if (operator == '/') {
                if (value == 0) throw new ArithmeticException("Деление на ноль");
                result /= value;
            }
        }

        return result;
    }

    private double parseFactor(double xValue) {
        char currentChar = chars[position];

        // Обработка отрицательных чисел
        if (currentChar == '-') {
            position++;
            return -parseFactor(xValue);
        }

        // Обработка скобок
        if (currentChar == '(') {
            position++;
            double result = parseExpression(xValue);
            if (position < chars.length && chars[position] == ')') {
                position++;
                return checkPower(result, xValue);
            }
            throw new IllegalArgumentException("Отсутствует закрывающая скобка");
        }

        // Обработка функций
        if (Character.isLetter(currentChar)) {
            if (currentChar == 'x') {
                position++;
                return checkPower(xValue, xValue);
            }
            return parseFunction(xValue);
        }

        // Обработка чисел
        return parseNumber(xValue);
    }

    private double parseNumber(double xValue) {
        StringBuilder sb = new StringBuilder();

        // Собираем число
        while (position < chars.length &&
                (Character.isDigit(chars[position]) || chars[position] == '.')) {
            sb.append(chars[position]);
            position++;
        }

        if (sb.length() == 0) {
            throw new IllegalArgumentException("Ожидалось число на позиции " + position);
        }

        double result = Double.parseDouble(sb.toString());
        return checkPower(result, xValue);
    }

    private double parseFunction(double xValue) {
        StringBuilder funcName = new StringBuilder();

        // Читаем имя функции
        while (position < chars.length && Character.isLetter(chars[position])) {
            funcName.append(chars[position]);
            position++;
        }

        // Проверяем скобку
        if (position >= chars.length || chars[position] != '(') {
            throw new IllegalArgumentException("Ожидалась открывающая скобка после " + funcName);
        }

        position++; // пропускаем '('
        double argument = parseExpression(xValue);

        if (position >= chars.length || chars[position] != ')') {
            throw new IllegalArgumentException("Ожидалась закрывающая скобка");
        }

        position++; // пропускаем ')'

        // Вычисляем значение функции
        double result = switch (funcName.toString()) {
            case "sin" -> Math.sin(argument);
            case "cos" -> Math.cos(argument);
            case "tan" -> Math.tan(argument);
            case "log" -> Math.log10(argument);
            case "ln" -> Math.log(argument);
            case "sqrt" -> Math.sqrt(argument);
            default -> throw new IllegalArgumentException("Неизвестная функция: " + funcName);
        };

        return checkPower(result, xValue);
    }

    private double checkPower(double base, double xValue) {
        // Проверяем степень
        if (position < chars.length && chars[position] == '^') {
            position++;
            double exponent = parseFactor(xValue);
            return Math.pow(base, exponent);
        }
        return base;
    }
}