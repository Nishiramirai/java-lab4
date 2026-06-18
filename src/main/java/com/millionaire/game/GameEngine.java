package com.millionaire.game;

import com.millionaire.model.Question;

import java.util.EnumSet;
import java.util.Set;

/**
 * Игровая логика «Кто хочет стать миллионером?», не зависящая от интерфейса.
 *
 * <p>Управляет текущим уровнем (1..15), денежной лестницей, несгораемой суммой,
 * учётом использованных подсказок и определением результата при ответе.</p>
 *
 * <p>Правила: для победы нужно верно ответить на 15 вопросов возрастающей
 * сложности. Несгораемую сумму игрок выбирает до начала игры — она остаётся у
 * игрока даже при неверном ответе на один из последующих вопросов.</p>
 */
public class GameEngine {

    /** Подсказки. Всего пять, но за игру разрешено использовать только четыре. */
    public enum Lifeline {
        FIFTY_FIFTY("50 на 50"),
        AUDIENCE("Помощь зала"),
        PHONE("Звонок другу"),
        DOUBLE_DIP("Право на ошибку"),
        SWITCH("Замена вопроса");

        private final String title;

        Lifeline(String title) {
            this.title = title;
        }

        public String getTitle() {
            return title;
        }
    }

    /** Максимальное число подсказок, которые можно задействовать за игру. */
    public static final int MAX_LIFELINES = 4;

    private final QuestionProvider provider;

    private int currentLevel;          // уровень текущего вопроса 1..15
    private Question currentQuestion;
    private long guaranteedAmount;     // выбранная несгораемая сумма
    private boolean finished;

    private final Set<Lifeline> usedLifelines = EnumSet.noneOf(Lifeline.class);
    private boolean doubleDipActive;   // активна ли «право на ошибку» для текущего вопроса
    private boolean firstMistakeMade;  // уже использован один неверный ответ по «праву на ошибку»

    public GameEngine(QuestionProvider provider) {
        this.provider = provider;
    }

    /**
     * Начинает новую партию.
     *
     * @param guaranteedAmount несгораемая сумма из денежной лестницы
     * @throws Exception если не удалось получить первый вопрос
     */
    public void start(long guaranteedAmount) throws Exception {
        this.guaranteedAmount = guaranteedAmount;
        this.currentLevel = 1;
        this.finished = false;
        this.usedLifelines.clear();
        this.doubleDipActive = false;
        this.firstMistakeMade = false;
        loadQuestion();
    }

    private void loadQuestion() throws Exception {
        Question q = provider.getQuestion(currentLevel);
        if (q == null) {
            throw new IllegalStateException("Нет вопросов уровня " + currentLevel);
        }
        this.currentQuestion = q;
        this.doubleDipActive = false;
        this.firstMistakeMade = false;
    }

    /**
     * Обрабатывает ответ игрока.
     *
     * @param answerIndex индекс выбранного варианта 0..3
     * @return результат хода
     * @throws Exception если при переходе к следующему вопросу его не удалось получить
     */
    public AnswerResult answer(int answerIndex) throws Exception {
        if (finished) {
            throw new IllegalStateException("Игра уже завершена");
        }
        boolean correct = currentQuestion.isCorrect(answerIndex);

        if (correct) {
            long prize = Prizes.prizeForLevel(currentLevel);
            if (currentLevel >= Prizes.LEVELS) {
                finished = true;
                return AnswerResult.win(prize);
            }
            currentLevel++;
            loadQuestion();
            return AnswerResult.correct(Prizes.securedBeforeLevel(currentLevel));
        }

        // Неверный ответ.
        if (doubleDipActive && !firstMistakeMade) {
            firstMistakeMade = true;
            return AnswerResult.secondChance();
        }

        finished = true;
        long consolation = computeConsolation();
        return AnswerResult.lose(consolation);
    }

    /** Игрок забирает текущий выигрыш и выходит из игры. */
    public AnswerResult walkAway() {
        finished = true;
        return AnswerResult.walkedAway(Prizes.securedBeforeLevel(currentLevel));
    }

    /**
     * Несгораемая сумма: остаётся у игрока, если он уже её «прошёл».
     */
    private long computeConsolation() {
        long secured = Prizes.securedBeforeLevel(currentLevel);
        return secured >= guaranteedAmount ? guaranteedAmount : 0;
    }

    // --- Подсказки ---------------------------------------------------------

    public boolean canUseLifeline(Lifeline l) {
        return !finished
                && !usedLifelines.contains(l)
                && usedLifelines.size() < MAX_LIFELINES;
    }

    public boolean isLifelineUsed(Lifeline l) {
        return usedLifelines.contains(l);
    }

    public int lifelinesUsed() {
        return usedLifelines.size();
    }

    private void markUsed(Lifeline l) {
        usedLifelines.add(l);
    }

    /** Подсказка «50 на 50»: возвращает индексы (0..3) двух убираемых неверных ответов. */
    public int[] useFiftyFifty(java.util.Random rnd) {
        markUsed(Lifeline.FIFTY_FIFTY);
        int right = currentQuestion.getRightIndex();
        int[] wrong = new int[3];
        int k = 0;
        for (int i = 0; i < 4; i++) {
            if (i != right) {
                wrong[k++] = i;
            }
        }
        // случайно оставляем один неверный, два других убираем
        for (int i = wrong.length - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            int t = wrong[i];
            wrong[i] = wrong[j];
            wrong[j] = t;
        }
        return new int[]{wrong[0], wrong[1]};
    }

    /**
     * Подсказка «Помощь зала»: распределение голосов зрителей по 4 вариантам.
     * Правильный ответ получает повышенный вес.
     */
    public int[] useAudienceHelp(java.util.Random rnd) {
        markUsed(Lifeline.AUDIENCE);
        int right = currentQuestion.getRightIndex();
        int[] weights = new int[4];
        int total = 0;
        for (int i = 0; i < 4; i++) {
            weights[i] = (i == right ? 45 + rnd.nextInt(35) : 5 + rnd.nextInt(20));
            total += weights[i];
        }
        int[] percent = new int[4];
        int acc = 0;
        for (int i = 0; i < 3; i++) {
            percent[i] = Math.round(weights[i] * 100f / total);
            acc += percent[i];
        }
        percent[3] = 100 - acc; // чтобы в сумме было ровно 100%
        return percent;
    }

    /**
     * Подсказка «Звонок другу»: с высокой вероятностью друг советует верный ответ.
     *
     * @return индекс варианта 0..3, который советует друг
     */
    public int usePhoneFriend(java.util.Random rnd) {
        markUsed(Lifeline.PHONE);
        if (rnd.nextInt(100) < 75) {
            return currentQuestion.getRightIndex();
        }
        return rnd.nextInt(4);
    }

    /** Подсказка «Право на ошибку»: разрешает один неверный ответ на текущем вопросе. */
    public void useDoubleDip() {
        markUsed(Lifeline.DOUBLE_DIP);
        doubleDipActive = true;
        firstMistakeMade = false;
    }

    /** Подсказка «Замена вопроса»: заменяет вопрос на другой того же уровня. */
    public void useSwitchQuestion() throws Exception {
        markUsed(Lifeline.SWITCH);
        loadQuestion();
    }

    // --- Геттеры -----------------------------------------------------------

    public int getCurrentLevel() {
        return currentLevel;
    }

    public Question getCurrentQuestion() {
        return currentQuestion;
    }

    public long getGuaranteedAmount() {
        return guaranteedAmount;
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isDoubleDipActive() {
        return doubleDipActive;
    }

    /** Текущий гарантированный выигрыш (за последний верно отвеченный вопрос). */
    public long getCurrentWinnings() {
        return Prizes.securedBeforeLevel(currentLevel);
    }
}
