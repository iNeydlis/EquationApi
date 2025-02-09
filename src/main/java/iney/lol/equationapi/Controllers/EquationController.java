package iney.lol.equationapi.Controllers;

import iney.lol.equationapi.utils.EquationParser;
import org.springframework.web.bind.annotation.*;
import lombok.Data;
import lombok.AllArgsConstructor;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.*;
import java.util.function.DoubleUnaryOperator;

@RestController
@RequestMapping("/api/equation")
public class EquationController {

    @Data
    @AllArgsConstructor
    public static class SolutionResult {
        private double root;
        private int iterations;
        private double accuracy;
        private double executionTime;
        private String method;
    }

    @Data
    public static class RootSeparationResult {
        private List<double[]> intervals;
        private List<Double> xPoints;
        private List<Double> yPoints;
    }

    // Задание 1: Отделение корней графическим методом
    @GetMapping("/task1")
    public RootSeparationResult separateRoots(
            @RequestParam String equation,
            @RequestParam double start,
            @RequestParam double end,
            @RequestParam double step) {

        RootSeparationResult result = new RootSeparationResult();
        List<double[]> intervals = new ArrayList<>();
        List<Double> xPoints = new ArrayList<>();
        List<Double> yPoints = new ArrayList<>();

        // Генерация точек для построения графика
        double x = start;
        while (x <= end) {
            xPoints.add(x);
            yPoints.add(evaluate(equation, x));
            x += step;
        }

        // Поиск интервалов с корнями
        for (int i = 0; i < xPoints.size() - 1; i++) {
            if (yPoints.get(i) * yPoints.get(i + 1) <= 0) {
                intervals.add(new double[]{xPoints.get(i), xPoints.get(i + 1)});
            }
        }

        result.setIntervals(intervals);
        result.setXPoints(xPoints);
        result.setYPoints(yPoints);
        return result;
    }

    // Задание 2: Метод половинного деления
    @GetMapping("/task2")
    public SolutionResult bisectionMethod(
            @RequestParam String equation,
            @RequestParam double a,
            @RequestParam double b,
            @RequestParam double tolerance) {

        if (a >= b) {
            throw new IllegalArgumentException("Левая граница интервала должна быть меньше правой");
        }

        long startTime = System.nanoTime();
        int iterations = 0;
        double c;

        double fa = evaluate(equation, a);
        double fb = evaluate(equation, b);

        // Проверяем знаки на концах интервала
        if (fa * fb >= 0) {
            throw new IllegalArgumentException(
                    String.format("Функция должна иметь разные знаки на концах интервала: f(%.2f)=%.2f, f(%.2f)=%.2f",
                            a, fa, b, fb)
            );
        }

        do {
            iterations++;
            c = (a + b) / 2;
            double fc = evaluate(equation, c);

            if (Math.abs(fc) < tolerance) {
                break;
            }

            if (fc * fa < 0) {
                b = c;
                fb = fc;
            } else {
                a = c;
                fa = fc;
            }

            if (iterations > 100) {
                throw new RuntimeException("Метод не сходится после 100 итераций");
            }
        } while (Math.abs(b - a) >= tolerance);

        double finalRoot = (a + b) / 2;
        double finalAccuracy = Math.abs(evaluate(equation, finalRoot));
        long endTime = System.nanoTime();

        return new SolutionResult(
                finalRoot,
                iterations,
                finalAccuracy,
                (endTime - startTime) / 1e6,
                "Метод половинного деления"
        );
    }

    // Задание 3: Метод простой итерации
    @GetMapping("/task3")
    public SolutionResult iterationMethod(
            @RequestParam String equation,
            @RequestParam double initialGuess,
            @RequestParam double tolerance) {

        long startTime = System.nanoTime();
        int iterations = 0;
        double x = initialGuess;
        double prevX;

        do {
            iterations++;
            prevX = x;
            double lambda = 0.1;
            x = x - evaluate(equation, x) * lambda;  // простая итерационная формула

            if (iterations > 1000) {
                throw new RuntimeException("Метод не сходится после 1000 итераций");
            }
        } while (Math.abs(x - prevX) >= tolerance);

        long endTime = System.nanoTime();

        return new SolutionResult(
                x,
                iterations,
                Math.abs(evaluate(equation, x)),
                (endTime - startTime) / 1e6,
                "Метод простой итерации"
        );
    }

    // Задание 4: Комбинированный метод
    @GetMapping("/task4")
    public SolutionResult combinedMethod(
            @RequestParam String equation,
            @RequestParam double a,
            @RequestParam double b,
            @RequestParam double tolerance) {

        long startTime = System.nanoTime();
        int iterations = 0;
        double x = (a + b) / 2; // начинаем с середины интервала

        // Проверяем граничные значения
        double fa = evaluate(equation, a);
        double fb = evaluate(equation, b);
        if (Math.abs(fa) < tolerance) return new SolutionResult(a, 1, Math.abs(fa), 0, "Комбинированный метод");
        if (Math.abs(fb) < tolerance) return new SolutionResult(b, 1, Math.abs(fb), 0, "Комбинированный метод");

        // Проверяем знаки на концах интервала
        if (fa * fb >= 0) {
            throw new IllegalArgumentException("Функция должна иметь разные знаки на концах интервала");
        }

        double prevX;
        do {
            iterations++;
            prevX = x;

            double fx = evaluate(equation, x);

            // Вычисляем производную
            double dfx = derivative(equation, x);

            // Защита от деления на очень маленькие числа
            if (Math.abs(dfx) < 1e-10) {
                dfx = Math.signum(dfx) * 1e-10;
            }

            // Метод Ньютона с ограничением шага
            double newtonStep = fx / dfx;
            if (Math.abs(newtonStep) > (b - a) / 2) {
                newtonStep = Math.signum(newtonStep) * (b - a) / 2;
            }
            double newtonX = x - newtonStep;

            // Метод хорд
            double denominator = fb - fa;
            if (Math.abs(denominator) < 1e-10) {
                denominator = Math.signum(denominator) * 1e-10;
            }
            double chordX = a - fa * (b - a) / denominator;

            // Выбираем новую точку как среднее между методами
            x = (newtonX + chordX) / 2;

            // Если точка вышла за пределы интервала, возвращаем её обратно
            if (x <= a || x >= b) {
                x = (a + b) / 2;
            }

            // Обновляем границы
            fx = evaluate(equation, x);
            if (fx * fa < 0) {
                b = x;
                fb = fx;
            } else {
                a = x;
                fa = fx;
            }

            if (iterations > 100) {
                throw new RuntimeException("Метод не сходится после 100 итераций");
            }

        } while (Math.abs(x - prevX) >= tolerance && Math.abs(evaluate(equation, x)) >= tolerance);

        double finalAccuracy = Math.abs(evaluate(equation, x));
        long endTime = System.nanoTime();

        return new SolutionResult(
                x,
                iterations,
                finalAccuracy,
                (endTime - startTime) / 1e6,
                "Комбинированный метод"
        );
    }


    // Задание 5: Сравнение всех методов
    @GetMapping("/task5")
    public List<SolutionResult> compareAllMethods(
            @RequestParam String equation,
            @RequestParam double a,
            @RequestParam double b,
            @RequestParam double tolerance) {

        List<SolutionResult> results = new ArrayList<>();

        try {
            results.add(bisectionMethod(equation, a, b, tolerance));
        } catch (Exception e) {
            results.add(new SolutionResult(0, 0, 0, 0,
                    "Метод половинного деления: " + e.getMessage()));
        }

        try {
            results.add(iterationMethod(equation, (a + b)/2, tolerance));
        } catch (Exception e) {
            results.add(new SolutionResult(0, 0, 0, 0,
                    "Метод простой итерации: " + e.getMessage()));
        }

        try {
            results.add(combinedMethod(equation, a, b, tolerance));
        } catch (Exception e) {
            results.add(new SolutionResult(0, 0, 0, 0,
                    "Комбинированный метод: " + e.getMessage()));
        }

        return results;
    }
    private double evaluate(String equation, double x) {
        return EquationParser.evaluate(equation, x);
    }
    private double evaluateExpression(String expression, double x) {
        // Заменяем x на его значение
        expression = expression.replaceAll("x", String.format("%.10f", x));

        // Обрабатываем функции
        expression = processMathFunctions(expression);

        Stack<Double> numbers = new Stack<>();
        Stack<Character> operators = new Stack<>();

        int i = 0;
        while (i < expression.length()) {
            char c = expression.charAt(i);

            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }

            if (Character.isDigit(c) || c == '.' || (c == '-' && (i == 0 || expression.charAt(i-1) == '('))) {
                StringBuilder numBuilder = new StringBuilder();
                boolean hasDecimalPoint = false;

                // Если это отрицательное число
                if (c == '-') {
                    numBuilder.append(c);
                    i++;
                }

                // Собираем число
                while (i < expression.length() &&
                        (Character.isDigit(expression.charAt(i)) ||
                                expression.charAt(i) == '.')) {
                    if (expression.charAt(i) == '.') {
                        if (hasDecimalPoint) {
                            throw new IllegalArgumentException("Invalid number format: multiple decimal points");
                        }
                        hasDecimalPoint = true;
                    }
                    numBuilder.append(expression.charAt(i));
                    i++;
                }

                if (!numBuilder.isEmpty()) {
                    numbers.push(Double.parseDouble(numBuilder.toString()));
                }
                continue;
            }

            if (c == '(') {
                operators.push(c);
            }
            else if (c == ')') {
                while (!operators.isEmpty() && operators.peek() != '(') {
                    numbers.push(applyOperator(operators.pop(), numbers.pop(), numbers.pop()));
                }
                if (!operators.isEmpty()) {
                    operators.pop(); // Remove '('
                }
            }
            else if (isOperator(c)) {
                while (!operators.isEmpty() && operators.peek() != '(' &&
                        precedence(operators.peek()) >= precedence(c)) {
                    numbers.push(applyOperator(operators.pop(), numbers.pop(), numbers.pop()));
                }
                operators.push(c);
            }

            i++;
        }

        while (!operators.isEmpty()) {
            numbers.push(applyOperator(operators.pop(), numbers.pop(), numbers.pop()));
        }

        return numbers.isEmpty() ? 0 : numbers.pop();
    }

    private String processMathFunctions(String expression) {
        expression = processTrigFunction(expression, "sin", Math::sin);
        expression = processTrigFunction(expression, "cos", Math::cos);
        expression = processTrigFunction(expression, "tan", Math::tan);
        expression = processTrigFunction(expression, "ln", Math::log);
        expression = processTrigFunction(expression, "log", Math::log10);
        return expression;
    }

    private String processTrigFunction(String expression, String funcName, DoubleUnaryOperator operator) {
        while (true) {
            int funcStart = expression.indexOf(funcName + "(");
            if (funcStart == -1) break;

            int bracketCount = 1;
            int i = funcStart + funcName.length() + 1;
            StringBuilder innerExpr = new StringBuilder();

            while (i < expression.length() && bracketCount > 0) {
                if (expression.charAt(i) == '(') bracketCount++;
                if (expression.charAt(i) == ')') bracketCount--;
                if (bracketCount > 0) innerExpr.append(expression.charAt(i));
                i++;
            }

            if (bracketCount == 0) {
                double innerValue = evaluateExpression(innerExpr.toString(), 0);
                double result = operator.applyAsDouble(innerValue);
                expression = expression.substring(0, funcStart) +
                        String.format("%.10f", result) +
                        expression.substring(i);
            } else {
                throw new IllegalArgumentException("Mismatched parentheses in " + funcName + " function");
            }
        }
        return expression;
    }

    private double derivative(String equation, double x) {
        double h = Math.sqrt(Math.ulp(x));  // малый шаг
        double forward = evaluate(equation, x + h);
        double backward = evaluate(equation, x - h);

        // Если производная слишком мала, возвращаем минимальное допустимое значение
        double deriv = (forward - backward) / (2 * h);
        if (Math.abs(deriv) < 1e-10) {
            return Math.signum(deriv) * 1e-10;
        }
        return deriv;
    }

    private boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '^';
    }

    private int precedence(char op) {
        return switch (op) {
            case '+', '-' -> 1;
            case '*', '/' -> 2;
            case '^' -> 3;
            default -> -1;
        };
    }

    private double applyOperator(char op, double b, double a) {
        return switch (op) {
            case '+' -> a + b;
            case '-' -> a - b;
            case '*' -> a * b;
            case '/' -> a / b;
            case '^' -> Math.pow(a, b);
            default -> 0;
        };
    }
}