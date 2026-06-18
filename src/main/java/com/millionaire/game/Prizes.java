package com.millionaire.game;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * Денежная лестница игры. 15 уровней, суммы возрастают от 500 до 3 000 000.
 * Суммы являются заменяемыми (не суммируются между уровнями).
 */
public final class Prizes {

    /** Стоимость верного ответа на вопрос уровня (i+1). */
    public static final long[] LADDER = {
            500, 1_000, 2_000, 3_000, 5_000,
            10_000, 15_000, 25_000, 50_000, 100_000,
            200_000, 400_000, 800_000, 1_500_000, 3_000_000
    };

    public static final int LEVELS = LADDER.length; // 15

    private static final DecimalFormat FMT;

    static {
        DecimalFormatSymbols sym = new DecimalFormatSymbols();
        sym.setGroupingSeparator(' '); // разряды через пробел: 3 000 000
        FMT = new DecimalFormat("#,###", sym);
    }

    private Prizes() {
    }

    /**
     * Сумма, заработанная после верного ответа на вопрос данного уровня.
     *
     * @param level уровень 1..15
     */
    public static long prizeForLevel(int level) {
        return LADDER[level - 1];
    }

    /**
     * Гарантированно заработанная сумма перед вопросом данного уровня
     * (то есть выигрыш за предыдущий верно отвеченный вопрос).
     *
     * @param level уровень текущего вопроса 1..15
     */
    public static long securedBeforeLevel(int level) {
        return level <= 1 ? 0 : LADDER[level - 2];
    }

    /** Форматирует сумму с разделением разрядов: {@code 3 000 000 руб.} */
    public static String format(long amount) {
        return FMT.format(amount) + " руб.";
    }
}
