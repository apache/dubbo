public class Calculator {
    // Method to add two numbers
    public double add(double a, double b) {
        return a + b;
    }

    // Method to subtract two numbers
    public double subtract(double a, double b) {
        return a - b;
    }

    // Method to multiply two numbers
    public double multiply(double a, double b) {
        return a * b;
    }

    // Method to divide two numbers
    public double divide(double a, double b) {
        if (b == 0) {
            throw new ArithmeticException("Cannot divide by zero");
        }
        return a / b;
    }
}
