package com.example.mathwake;

import org.junit.Test;

import java.util.Random;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class MathQuestionGeneratorTest {
    @Test public void additionIsCorrect() { assertBinary(AlarmModel.OP_ADD, " + "); }
    @Test public void subtractionIsCorrectAndNonNegative() {
        MathQuestionGenerator g = generator();
        for (int i = 0; i < 200; i++) {
            MathQuestionGenerator.Question q = g.generate(AlarmModel.DIFFICULTY_HARD, AlarmModel.OP_SUBTRACT);
            int[] values = operands(q, " − ");
            assertEquals(values[0] - values[1], q.getAnswer());
            assertTrue(q.getAnswer() >= 0);
        }
    }
    @Test public void multiplicationIsCorrect() { assertBinary(AlarmModel.OP_MULTIPLY, " × "); }
    @Test public void divisionHasIntegerResultAndNoZeroDivisor() {
        MathQuestionGenerator g = generator();
        for (int i = 0; i < 200; i++) {
            MathQuestionGenerator.Question q = g.generate(AlarmModel.DIFFICULTY_HARD, AlarmModel.OP_DIVIDE);
            String[] parts = q.getText().replace(" = ?", "").split(" ÷ ");
            int dividend = Integer.parseInt(parts[0]), divisor = Integer.parseInt(parts[1]);
            assertNotEquals(0, divisor); assertEquals(0, dividend % divisor); assertEquals(dividend / divisor, q.getAnswer());
        }
    }
    @Test public void combinedUsesCorrectPrecedence() {
        MathQuestionGenerator g = generator();
        for (int i = 0; i < 100; i++) {
            MathQuestionGenerator.Question q = g.generate(AlarmModel.DIFFICULTY_HARD, "COMBINED");
            String expression = q.getText().replace(" = ?", "");
            int expected;
            if (expression.startsWith("(")) {
                String[] parts = expression.replace("(", "").replace(")", "").split(" − | ÷ ");
                expected = (Integer.parseInt(parts[0]) - Integer.parseInt(parts[1])) / Integer.parseInt(parts[2]);
            } else {
                String[] parts = expression.split(" × | − ");
                expected = Integer.parseInt(parts[0]) * Integer.parseInt(parts[1]) - Integer.parseInt(parts[2]);
            }
            assertEquals(expected, q.getAnswer());
            assertTrue(q.getAnswer() >= 0 && q.getAnswer() < 1000);
        }
    }
    @Test public void questionsDoNotRepeatConsecutively() {
        MathQuestionGenerator g = generator(); String previous = "";
        for (int i = 0; i < 300; i++) {
            String current = g.generate(AlarmModel.DIFFICULTY_EASY, AlarmModel.OP_RANDOM).getText();
            assertNotEquals(previous, current); previous = current;
        }
    }
    @Test public void difficultyRangesAreReasonable() {
        MathQuestionGenerator g = generator();
        for (int i = 0; i < 200; i++) {
            assertTrue(Math.abs(g.generate(AlarmModel.DIFFICULTY_EASY, AlarmModel.OP_RANDOM).getAnswer()) <= 400);
            assertTrue(Math.abs(g.generate(AlarmModel.DIFFICULTY_HARD, AlarmModel.OP_RANDOM).getAnswer()) < 1000);
        }
    }

    private void assertBinary(String operation, String separator) {
        MathQuestionGenerator g = generator();
        for (int i = 0; i < 50; i++) {
            MathQuestionGenerator.Question q = g.generate(AlarmModel.DIFFICULTY_MEDIUM, operation);
            int[] values = operands(q, separator);
            int expected = AlarmModel.OP_ADD.equals(operation) ? values[0] + values[1] : values[0] * values[1];
            assertEquals(operation, q.getOperation());
            assertEquals(expected, q.getAnswer());
        }
    }
    private int[] operands(MathQuestionGenerator.Question q, String separator) {
        String[] parts = q.getText().replace(" = ?", "").split(Pattern.quote(separator));
        return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
    }
    private MathQuestionGenerator generator() { return new MathQuestionGenerator(new Random(123456L)); }
}
