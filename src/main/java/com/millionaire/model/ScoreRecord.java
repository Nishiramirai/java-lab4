package com.millionaire.model;

/**
 * Запись таблицы рекордов: результат одной сыгранной партии.
 */
public class ScoreRecord {

    private final String playerName;
    private final long prize;          // выигранная сумма
    private final int levelReached;    // достигнутый уровень (число верных ответов)
    private final boolean won;         // true, если пройдены все 15 вопросов
    private final String playedAt;     // дата/время партии (ISO-8601)

    public ScoreRecord(String playerName, long prize, int levelReached, boolean won, String playedAt) {
        this.playerName = playerName;
        this.prize = prize;
        this.levelReached = levelReached;
        this.won = won;
        this.playedAt = playedAt;
    }

    public String getPlayerName() {
        return playerName;
    }

    public long getPrize() {
        return prize;
    }

    public int getLevelReached() {
        return levelReached;
    }

    public boolean isWon() {
        return won;
    }

    public String getPlayedAt() {
        return playedAt;
    }
}
