package com.millionaire.ui;

import com.millionaire.db.Database;
import com.millionaire.game.AnswerResult;
import com.millionaire.game.DbQuestionProvider;
import com.millionaire.game.GameEngine;
import com.millionaire.game.Prizes;
import com.millionaire.model.Question;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.net.URL;
import java.util.Random;

/**
 * Главное окно игры «Кто хочет стать миллионером?».
 *
 * <p>Связывает игровую логику ({@link GameEngine}) с элементами управления:
 * вопрос и четыре кнопки ответов, денежная лестница, пять подсказок, табло
 * выигрыша, меню новой игры и таблицы рекордов. Вопросы берутся из базы данных
 * SQLite.</p>
 */
public class GameFrame extends JFrame {

    private final Database db;
    private final Random rnd = new Random();

    private GameEngine engine;
    private String playerName = "Игрок";

    // --- Элементы интерфейса ---
    private final JLabel lblQuestion = new JLabel("", SwingConstants.CENTER);
    private final JButton[] answerButtons = new JButton[4];
    private final JLabel lblWinnings = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel lblLevel = new JLabel(" ", SwingConstants.CENTER);
    private final PrizeLadderPanel ladderPanel = new PrizeLadderPanel();
    private final JButton btnWalkAway = new JButton("Забрать деньги");

    private final JButton btnFiftyFifty = new JButton(GameEngine.Lifeline.FIFTY_FIFTY.getTitle());
    private final JButton btnAudience = new JButton(GameEngine.Lifeline.AUDIENCE.getTitle());
    private final JButton btnPhone = new JButton(GameEngine.Lifeline.PHONE.getTitle());
    private final JButton btnDoubleDip = new JButton(GameEngine.Lifeline.DOUBLE_DIP.getTitle());
    private final JButton btnSwitch = new JButton(GameEngine.Lifeline.SWITCH.getTitle());

    /** Варианты, скрытые подсказкой «50 на 50» (не должны включаться обратно). */
    private final boolean[] hiddenByFifty = new boolean[4];

    public GameFrame(Database db) {
        super("Кто хочет стать миллионером?");
        this.db = db;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 600));
        setJMenuBar(buildMenuBar());

        add(buildHeader(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(ladderPanel, BorderLayout.EAST);
        add(buildSouth(), BorderLayout.SOUTH);

        setLocationRelativeTo(null);
        setAnswersEnabled(false);
        setLifelinesEnabled(false);
        btnWalkAway.setEnabled(false);
    }

    // --- Построение интерфейса ----------------------------------------------

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu gameMenu = new JMenu("Игра");
        JMenuItem newGame = new JMenuItem("Новая игра");
        JMenuItem records = new JMenuItem("Таблица рекордов");
        JMenuItem exit = new JMenuItem("Выход");
        newGame.addActionListener(this::onNewGame);
        records.addActionListener(e -> new RecordsDialog(this, db).setVisible(true));
        exit.addActionListener(e -> dispose());
        gameMenu.add(newGame);
        gameMenu.add(records);
        gameMenu.addSeparator();
        gameMenu.add(exit);

        bar.add(gameMenu);
        return bar;
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(10, 20, 60));
        header.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel logo = new JLabel();
        URL logoUrl = GameFrame.class.getResource("/logo.jpg");
        if (logoUrl != null) {
            Image img = new ImageIcon(logoUrl).getImage()
                    .getScaledInstance(160, 100, Image.SCALE_SMOOTH);
            logo.setIcon(new ImageIcon(img));
        }
        header.add(logo, BorderLayout.WEST);

        JLabel title = new JLabel("Кто хочет стать миллионером?", SwingConstants.CENTER);
        title.setFont(new Font("Serif", Font.BOLD, 28));
        title.setForeground(new Color(245, 200, 60));
        header.add(title, BorderLayout.CENTER);

        return header;
    }

    private JPanel buildCenter() {
        JPanel center = new JPanel(new BorderLayout(10, 10));
        center.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        lblQuestion.setFont(new Font("SansSerif", Font.BOLD, 18));
        lblQuestion.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(40, 120, 220), 2, true),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)));
        lblQuestion.setPreferredSize(new Dimension(0, 140));
        center.add(lblQuestion, BorderLayout.NORTH);

        JPanel answers = new JPanel(new GridLayout(2, 2, 12, 12));
        for (int i = 0; i < 4; i++) {
            final int index = i;
            JButton btn = new JButton();
            btn.setFont(new Font("SansSerif", Font.PLAIN, 16));
            btn.setActionCommand(String.valueOf(i + 1));
            btn.addActionListener(e -> onAnswer(index));
            answerButtons[i] = btn;
            answers.add(btn);
        }
        center.add(answers, BorderLayout.CENTER);

        return center;
    }

    private JPanel buildSouth() {
        JPanel south = new JPanel(new BorderLayout(8, 8));
        south.setBorder(BorderFactory.createEmptyBorder(8, 12, 12, 12));

        // Подсказки
        JPanel lifelines = new JPanel(new GridLayout(1, 0, 8, 0));
        lifelines.setBorder(BorderFactory.createTitledBorder("Подсказки (доступно 4 из 5)"));
        btnFiftyFifty.addActionListener(e -> onFiftyFifty());
        btnAudience.addActionListener(e -> onAudience());
        btnPhone.addActionListener(e -> onPhone());
        btnDoubleDip.addActionListener(e -> onDoubleDip());
        btnSwitch.addActionListener(e -> onSwitch());
        lifelines.add(btnFiftyFifty);
        lifelines.add(btnAudience);
        lifelines.add(btnPhone);
        lifelines.add(btnDoubleDip);
        lifelines.add(btnSwitch);

        // Табло
        JPanel info = new JPanel(new GridLayout(1, 0, 8, 0));
        lblLevel.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblWinnings.setFont(new Font("SansSerif", Font.BOLD, 16));
        lblWinnings.setForeground(new Color(20, 120, 40));
        info.add(lblLevel);
        info.add(lblWinnings);
        btnWalkAway.addActionListener(e -> onWalkAway());
        info.add(btnWalkAway);

        south.add(lifelines, BorderLayout.NORTH);
        south.add(Box.createVerticalStrut(6), BorderLayout.CENTER);
        south.add(info, BorderLayout.SOUTH);
        return south;
    }

    // --- Обработчики меню ----------------------------------------------------

    private void onNewGame(ActionEvent e) {
        long guaranteed = askPlayerAndGuaranteed();
        if (guaranteed < 0) {
            return; // отмена
        }
        engine = new GameEngine(new DbQuestionProvider(db));
        try {
            engine.start(guaranteed);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Не удалось начать игру: " + ex.getMessage(),
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }
        setLifelinesEnabled(true);
        refreshLifelineButtons();
        refreshQuestion();
        setAnswersEnabled(true);
        btnWalkAway.setEnabled(true);
    }

    /** Запрашивает имя игрока и несгораемую сумму. @return сумма или -1 при отмене. */
    private long askPlayerAndGuaranteed() {
        JTextField nameField = new JTextField(playerName);
        JComboBox<String> guaranteedCombo = new JComboBox<>();
        for (long amount : Prizes.LADDER) {
            guaranteedCombo.addItem(Prizes.format(amount));
        }
        guaranteedCombo.setSelectedIndex(4); // по умолчанию 5 000

        JPanel panel = new JPanel(new GridLayout(0, 1, 6, 6));
        panel.add(new JLabel("Имя игрока:"));
        panel.add(nameField);
        panel.add(new JLabel("Несгораемая сумма:"));
        panel.add(guaranteedCombo);

        int res = JOptionPane.showConfirmDialog(this, panel, "Новая игра",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) {
            return -1;
        }
        playerName = nameField.getText().trim().isEmpty() ? "Игрок" : nameField.getText().trim();
        return Prizes.LADDER[guaranteedCombo.getSelectedIndex()];
    }

    // --- Обработчики игры ----------------------------------------------------

    private void onAnswer(int index) {
        if (engine == null || engine.isFinished()) {
            return;
        }
        Question current = engine.getCurrentQuestion();
        int rightIndex = current.getRightIndex();

        AnswerResult result;
        try {
            result = engine.answer(index);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Не удалось получить следующий вопрос: " + ex.getMessage(),
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }

        switch (result.getType()) {
            case CORRECT:
                refreshQuestion();
                setAnswersEnabled(true);
                refreshLifelineButtons();
                break;
            case SECOND_CHANCE:
                hiddenByFifty[index] = true; // выбывший неверный вариант
                answerButtons[index].setEnabled(false);
                JOptionPane.showMessageDialog(this,
                        "Неверно, но у вас есть право на ошибку — попробуйте ещё раз.",
                        "Право на ошибку", JOptionPane.WARNING_MESSAGE);
                break;
            case WIN:
                finishGame(result, true,
                        "Поздравляем! Вы выиграли главный приз — "
                                + Prizes.format(result.getPrize()) + "!");
                break;
            case LOSE:
                String correctText = (rightIndex + 1) + ". " + current.getAnswer(rightIndex);
                finishGame(result, false,
                        "Неверный ответ. Правильный: " + correctText
                                + "\nВаш выигрыш: " + Prizes.format(result.getPrize()));
                break;
            default:
                break;
        }
    }

    private void onWalkAway() {
        if (engine == null || engine.isFinished()) {
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Забрать " + Prizes.format(engine.getCurrentWinnings()) + " и завершить игру?",
                "Забрать деньги", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            AnswerResult result = engine.walkAway();
            finishGame(result, false,
                    "Вы забрали выигрыш: " + Prizes.format(result.getPrize()));
        }
    }

    private void finishGame(AnswerResult result, boolean won, String message) {
        db.saveRecord(playerName, result.getPrize(),
                won ? Prizes.LEVELS : engine.getCurrentLevel() - 1, won);
        setAnswersEnabled(false);
        setLifelinesEnabled(false);
        btnWalkAway.setEnabled(false);
        JOptionPane.showMessageDialog(this, message, "Игра окончена",
                won ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
    }

    // --- Подсказки -----------------------------------------------------------

    private void onFiftyFifty() {
        if (!ensureLifeline(GameEngine.Lifeline.FIFTY_FIFTY)) {
            return;
        }
        int[] toHide = engine.useFiftyFifty(rnd);
        for (int idx : toHide) {
            hiddenByFifty[idx] = true;
            answerButtons[idx].setEnabled(false);
            answerButtons[idx].setText("");
        }
        refreshLifelineButtons();
    }

    private void onAudience() {
        if (!ensureLifeline(GameEngine.Lifeline.AUDIENCE)) {
            return;
        }
        int[] percent = engine.useAudienceHelp(rnd);
        StringBuilder sb = new StringBuilder("<html>Результаты голосования зала:<br><br>");
        for (int i = 0; i < 4; i++) {
            sb.append(i + 1).append(": ");
            int bars = Math.round(percent[i] / 5f);
            for (int b = 0; b < bars; b++) {
                sb.append("█");
            }
            sb.append(" ").append(percent[i]).append("%<br>");
        }
        sb.append("</html>");
        JOptionPane.showMessageDialog(this, new JLabel(sb.toString()),
                "Помощь зала", JOptionPane.INFORMATION_MESSAGE);
        refreshLifelineButtons();
    }

    private void onPhone() {
        if (!ensureLifeline(GameEngine.Lifeline.PHONE)) {
            return;
        }
        int suggested = engine.usePhoneFriend(rnd);
        JOptionPane.showMessageDialog(this,
                "Друг считает, что правильный ответ — вариант № " + (suggested + 1)
                        + ":\n«" + engine.getCurrentQuestion().getAnswer(suggested) + "»",
                "Звонок другу", JOptionPane.INFORMATION_MESSAGE);
        refreshLifelineButtons();
    }

    private void onDoubleDip() {
        if (!ensureLifeline(GameEngine.Lifeline.DOUBLE_DIP)) {
            return;
        }
        engine.useDoubleDip();
        JOptionPane.showMessageDialog(this,
                "Право на ошибку активировано: на этом вопросе можно дать два ответа.",
                "Право на ошибку", JOptionPane.INFORMATION_MESSAGE);
        refreshLifelineButtons();
    }

    private void onSwitch() {
        if (!ensureLifeline(GameEngine.Lifeline.SWITCH)) {
            return;
        }
        try {
            engine.useSwitchQuestion();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Не удалось заменить вопрос: " + ex.getMessage(),
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }
        refreshQuestion();
        setAnswersEnabled(true);
        refreshLifelineButtons();
    }

    private boolean ensureLifeline(GameEngine.Lifeline l) {
        if (engine == null || engine.isFinished()) {
            return false;
        }
        if (!engine.canUseLifeline(l)) {
            JOptionPane.showMessageDialog(this,
                    "Эта подсказка недоступна (использовано максимум 4 из 5).",
                    "Подсказка", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    // --- Обновление интерфейса ----------------------------------------------

    private void refreshQuestion() {
        Question q = engine.getCurrentQuestion();
        lblQuestion.setText("<html><div style='text-align:center'>" + escape(q.getText()) + "</div></html>");
        String[] answers = q.getAnswers();
        for (int i = 0; i < 4; i++) {
            hiddenByFifty[i] = false; // новый вопрос — все варианты снова видимы
            answerButtons[i].setText((i + 1) + ". " + answers[i]);
            answerButtons[i].setEnabled(true);
        }
        lblLevel.setText("Вопрос " + engine.getCurrentLevel() + " из " + Prizes.LEVELS);
        lblWinnings.setText("Несгораемая сумма: " + Prizes.format(engine.getGuaranteedAmount())
                + "   |   Выигрыш: " + Prizes.format(engine.getCurrentWinnings()));
        ladderPanel.setState(engine.getCurrentLevel(), engine.getGuaranteedAmount());
    }

    private void setAnswersEnabled(boolean enabled) {
        for (int i = 0; i < answerButtons.length; i++) {
            answerButtons[i].setEnabled(enabled && !hiddenByFifty[i]);
        }
    }

    private void setLifelinesEnabled(boolean enabled) {
        btnFiftyFifty.setEnabled(enabled);
        btnAudience.setEnabled(enabled);
        btnPhone.setEnabled(enabled);
        btnDoubleDip.setEnabled(enabled);
        btnSwitch.setEnabled(enabled);
    }

    /** Делает недоступными использованные подсказки и все остальные, если задействованы 4. */
    private void refreshLifelineButtons() {
        btnFiftyFifty.setEnabled(engine.canUseLifeline(GameEngine.Lifeline.FIFTY_FIFTY));
        btnAudience.setEnabled(engine.canUseLifeline(GameEngine.Lifeline.AUDIENCE));
        btnPhone.setEnabled(engine.canUseLifeline(GameEngine.Lifeline.PHONE));
        btnDoubleDip.setEnabled(engine.canUseLifeline(GameEngine.Lifeline.DOUBLE_DIP));
        btnSwitch.setEnabled(engine.canUseLifeline(GameEngine.Lifeline.SWITCH));
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
