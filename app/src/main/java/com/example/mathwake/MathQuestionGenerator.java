package com.example.mathwake;

import java.util.Random;

/** Bộ sinh câu hỏi độc lập với Activity để dễ kiểm thử. */
public class MathQuestionGenerator {
    public static class Question {
        private final String text;
        private final int answer;
        private final String operation;
        Question(String text, int answer, String operation) {
            this.text = text; this.answer = answer; this.operation = operation;
        }
        public String getText() { return text; }
        public int getAnswer() { return answer; }
        public String getOperation() { return operation; }
    }

    private final Random random;
    private String previousText = "";

    public MathQuestionGenerator() { this(new Random()); }
    public MathQuestionGenerator(Random random) { this.random = random; }

    public void restorePreviousText(String text) { previousText = text == null ? "" : text; }

    public Question generate(String difficulty, String requestedOperation) {
        Question q = null;
        for (int i = 0; i < 20; i++) {
            String operation = requestedOperation;
            if (AlarmModel.OP_RANDOM.equals(operation)) {
                int choices = AlarmModel.DIFFICULTY_HARD.equals(difficulty) ? 5 : 4;
                operation = new String[]{AlarmModel.OP_ADD, AlarmModel.OP_SUBTRACT,
                        AlarmModel.OP_MULTIPLY, AlarmModel.OP_DIVIDE, "COMBINED"}[random.nextInt(choices)];
            }
            q = create(difficulty, operation);
            if (!q.getText().equals(previousText)) break;
        }
        previousText = q.getText();
        return q;
    }

    private Question create(String difficulty, String operation) {
        int max = AlarmModel.DIFFICULTY_EASY.equals(difficulty) ? 20
                : AlarmModel.DIFFICULTY_MEDIUM.equals(difficulty) ? 60 : 150;
        switch (operation) {
            case AlarmModel.OP_SUBTRACT: {
                int a = number(2, max), b = number(1, a);
                return new Question(a + " − " + b + " = ?", a - b, operation);
            }
            case AlarmModel.OP_MULTIPLY: {
                int factorMax = AlarmModel.DIFFICULTY_EASY.equals(difficulty) ? 10
                        : AlarmModel.DIFFICULTY_MEDIUM.equals(difficulty) ? 12 : 20;
                int a = number(2, factorMax), b = number(2, factorMax);
                return new Question(a + " × " + b + " = ?", a * b, operation);
            }
            case AlarmModel.OP_DIVIDE: {
                int divisorMax = AlarmModel.DIFFICULTY_EASY.equals(difficulty) ? 10 : 15;
                int divisor = number(2, divisorMax);
                int quotient = number(2, Math.max(2, max / divisor));
                return new Question((divisor * quotient) + " ÷ " + divisor + " = ?", quotient, operation);
            }
            case "COMBINED": {
                int a = number(2, 15), b = number(2, 12);
                if (random.nextBoolean()) {
                    int c = number(1, a * b);
                    return new Question(a + " × " + b + " − " + c + " = ?", a * b - c, operation);
                }
                int sum = a + b;
                int divisor = number(2, 8);
                return new Question("(" + sum * divisor + " − " + b * divisor + ") ÷ " + divisor + " = ?", a, operation);
            }
            case AlarmModel.OP_ADD:
            default: {
                int a = number(1, max), b = number(1, max);
                return new Question(a + " + " + b + " = ?", a + b, AlarmModel.OP_ADD);
            }
        }
    }

    private int number(int min, int max) { return min + random.nextInt(Math.max(1, max - min + 1)); }
}
