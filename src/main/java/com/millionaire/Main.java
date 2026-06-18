package com.millionaire;

import com.millionaire.db.Database;
import com.millionaire.ui.GameFrame;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Точка входа в приложение «Кто хочет стать миллионером?».
 *
 * <p>Инициализирует базу данных SQLite (при первом запуске импортирует вопросы
 * из ресурса {@code questions.txt}) и открывает главное окно игры.</p>
 */
public class Main {

    /** Имя файла базы данных в рабочем каталоге приложения. */
    private static final String DB_FILE = "WhoWantsToBeAMillionaire.db";

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // используем стандартный внешний вид
        }

        Database db = new Database(DB_FILE);
        int imported = db.importQuestionsIfEmpty();
        if (imported > 0) {
            System.out.println("Импортировано вопросов в базу данных: " + imported);
        }

        SwingUtilities.invokeLater(() -> {
            if (db.countQuestions() == 0) {
                JOptionPane.showMessageDialog(null,
                        "В базе данных нет вопросов. Проверьте файл questions.txt.",
                        "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }
            GameFrame frame = new GameFrame(db);
            frame.setVisible(true);
        });

        Runtime.getRuntime().addShutdownHook(new Thread(db::close));
    }
}
