package com.millionaire.game;

/**
 * Результат хода игрока.
 */
public class AnswerResult {

    public enum Type {
        CORRECT,        // верный ответ, игра продолжается
        SECOND_CHANCE,  // неверный ответ, но действует «право на ошибку»
        WIN,            // верно отвечены все 15 вопросов
        LOSE,           // неверный ответ, игра окончена
        WALKED_AWAY     // игрок забрал деньги
    }

    private final Type type;
    private final long prize; // сумма, относящаяся к исходу

    private AnswerResult(Type type, long prize) {
        this.type = type;
        this.prize = prize;
    }

    public static AnswerResult correct(long securedWinnings) {
        return new AnswerResult(Type.CORRECT, securedWinnings);
    }

    public static AnswerResult secondChance() {
        return new AnswerResult(Type.SECOND_CHANCE, 0);
    }

    public static AnswerResult win(long prize) {
        return new AnswerResult(Type.WIN, prize);
    }

    public static AnswerResult lose(long consolation) {
        return new AnswerResult(Type.LOSE, consolation);
    }

    public static AnswerResult walkedAway(long prize) {
        return new AnswerResult(Type.WALKED_AWAY, prize);
    }

    public Type getType() {
        return type;
    }

    public long getPrize() {
        return prize;
    }

    public boolean isGameOver() {
        return type == Type.WIN || type == Type.LOSE || type == Type.WALKED_AWAY;
    }
}
