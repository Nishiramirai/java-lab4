package com.millionaire.model;

import java.util.Arrays;

/**
 * Модель вопроса игры.
 *
 * <p>Хранит текст вопроса, четыре варианта ответа, номер правильного варианта
 * (1..4) и уровень сложности (1..15). Класс используется как при чтении вопросов
 * из текстового файла и из базы данных SQLite, так и при генерации вопросов
 * средствами ИИ.</p>
 */
public class Question {

    private final String text;
    private final String[] answers;   // ровно 4 варианта ответа
    private final int rightAnswer;    // номер правильного ответа: 1..4
    private final int level;          // уровень сложности: 1..15

    public Question(String text, String[] answers, int rightAnswer, int level) {
        if (answers == null || answers.length != 4) {
            throw new IllegalArgumentException("Вопрос должен содержать ровно 4 варианта ответа");
        }
        if (rightAnswer < 1 || rightAnswer > 4) {
            throw new IllegalArgumentException("Номер правильного ответа должен быть в диапазоне 1..4");
        }
        this.text = text;
        this.answers = Arrays.copyOf(answers, 4);
        this.rightAnswer = rightAnswer;
        this.level = level;
    }

    /**
     * Создаёт вопрос из строки текстового файла вида:
     * {@code Текст\tОтвет1\tОтвет2\tОтвет3\tОтвет4\tНомерПравильного\tУровень}.
     *
     * @param parts массив значений, разбитый по символу табуляции
     */
    public static Question fromTsv(String[] parts) {
        if (parts.length < 7) {
            throw new IllegalArgumentException("Ожидалось 7 полей, получено " + parts.length);
        }
        String[] ans = new String[]{parts[1], parts[2], parts[3], parts[4]};
        int right = Integer.parseInt(parts[5].trim());
        int lvl = Integer.parseInt(parts[6].trim());
        return new Question(parts[0], ans, right, lvl);
    }

    public String getText() {
        return text;
    }

    /** @return копия массива из 4 вариантов ответа */
    public String[] getAnswers() {
        return Arrays.copyOf(answers, 4);
    }

    /** @param index индекс варианта 0..3 */
    public String getAnswer(int index) {
        return answers[index];
    }

    /** @return номер правильного ответа 1..4 */
    public int getRightAnswer() {
        return rightAnswer;
    }

    /** @return индекс правильного ответа 0..3 */
    public int getRightIndex() {
        return rightAnswer - 1;
    }

    public int getLevel() {
        return level;
    }

    /** @param index индекс варианта 0..3 */
    public boolean isCorrect(int index) {
        return index == getRightIndex();
    }

    @Override
    public String toString() {
        return "[" + level + "] " + text;
    }
}
