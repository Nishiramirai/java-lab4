package com.millionaire.db;

import com.millionaire.model.Question;
import com.millionaire.model.ScoreRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Слой доступа к базе данных SQLite.
 *
 * <p>Отвечает за:
 * <ul>
 *   <li>создание схемы БД (таблицы вопросов и рекордов);</li>
 *   <li>первичный импорт вопросов из ресурса {@code questions.txt};</li>
 *   <li>выборку случайного вопроса заданного уровня;</li>
 *   <li>сохранение результата партии и выдачу TOP-10 игроков.</li>
 * </ul>
 *
 * <p>Файл базы данных {@code WhoWantsToBeAMillionaire.db} создаётся в рабочем
 * каталоге приложения при первом запуске.</p>
 */
public class Database implements AutoCloseable {

    private static final DateTimeFormatter TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Connection conn;

    public Database(String dbFileName) {
        try {
            // Явная регистрация драйвера — на случай старого механизма загрузки.
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:" + dbFileName);
            createSchema();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Не найден драйвер JDBC для SQLite", e);
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось открыть базу данных: " + dbFileName, e);
        }
    }

    private void createSchema() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS questions (" +
                    "  id           INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "  text         TEXT    NOT NULL," +
                    "  answer1      TEXT    NOT NULL," +
                    "  answer2      TEXT    NOT NULL," +
                    "  answer3      TEXT    NOT NULL," +
                    "  answer4      TEXT    NOT NULL," +
                    "  right_answer INTEGER NOT NULL," +   // 1..4
                    "  level        INTEGER NOT NULL" +    // 1..15
                    ")");
            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS records (" +
                    "  id            INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "  player_name   TEXT    NOT NULL," +
                    "  prize         INTEGER NOT NULL," +
                    "  level_reached INTEGER NOT NULL," +
                    "  won           INTEGER NOT NULL," +   // 0/1
                    "  played_at     TEXT    NOT NULL" +
                    ")");
        }
    }

    /** @return количество вопросов в таблице questions */
    public int countQuestions() {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM questions")) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Ошибка подсчёта вопросов", e);
        }
    }

    /**
     * Импортирует вопросы из ресурса {@code /questions.txt} (формат TSV), если
     * таблица вопросов пуста. Возвращает число импортированных строк.
     */
    public int importQuestionsIfEmpty() {
        if (countQuestions() > 0) {
            return 0;
        }
        int imported = 0;
        try (InputStream in = Database.class.getResourceAsStream("/questions.txt")) {
            if (in == null) {
                throw new IllegalStateException("Ресурс /questions.txt не найден");
            }
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8))) {
                conn.setAutoCommit(false);
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.trim().isEmpty()) {
                        continue;
                    }
                    String[] parts = line.split("\t");
                    if (parts.length < 7) {
                        continue; // пропускаем некорректные строки
                    }
                    addQuestion(Question.fromTsv(parts));
                    imported++;
                }
                conn.commit();
                conn.setAutoCommit(true);
            }
        } catch (IOException | SQLException e) {
            throw new IllegalStateException("Ошибка импорта вопросов из файла", e);
        }
        return imported;
    }

    /** Добавляет вопрос в базу данных. */
    public void addQuestion(Question q) {
        String sql = "INSERT INTO questions(text, answer1, answer2, answer3, answer4, " +
                "right_answer, level) VALUES(?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, q.getText());
            ps.setString(2, q.getAnswer(0));
            ps.setString(3, q.getAnswer(1));
            ps.setString(4, q.getAnswer(2));
            ps.setString(5, q.getAnswer(3));
            ps.setInt(6, q.getRightAnswer());
            ps.setInt(7, q.getLevel());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось сохранить вопрос", e);
        }
    }

    /**
     * Возвращает случайный вопрос заданного уровня сложности.
     *
     * @return вопрос либо {@code null}, если вопросов такого уровня нет
     */
    public Question getRandomQuestion(int level) {
        String sql = "SELECT text, answer1, answer2, answer3, answer4, right_answer, level " +
                "FROM questions WHERE level = ? ORDER BY RANDOM() LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, level);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                String[] ans = {
                        rs.getString("answer1"), rs.getString("answer2"),
                        rs.getString("answer3"), rs.getString("answer4")
                };
                return new Question(rs.getString("text"), ans,
                        rs.getInt("right_answer"), rs.getInt("level"));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Ошибка выборки вопроса уровня " + level, e);
        }
    }

    /** Сохраняет результат партии в таблицу рекордов. */
    public void saveRecord(String playerName, long prize, int levelReached, boolean won) {
        String sql = "INSERT INTO records(player_name, prize, level_reached, won, played_at) " +
                "VALUES(?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerName == null || playerName.isEmpty() ? "Игрок" : playerName);
            ps.setLong(2, prize);
            ps.setInt(3, levelReached);
            ps.setInt(4, won ? 1 : 0);
            ps.setString(5, LocalDateTime.now().format(TS));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось сохранить рекорд", e);
        }
    }

    /** @return TOP-N игроков по размеру выигрыша (по убыванию). */
    public List<ScoreRecord> getTopRecords(int limit) {
        List<ScoreRecord> result = new ArrayList<>();
        String sql = "SELECT player_name, prize, level_reached, won, played_at " +
                "FROM records ORDER BY prize DESC, played_at ASC LIMIT ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new ScoreRecord(
                            rs.getString("player_name"),
                            rs.getLong("prize"),
                            rs.getInt("level_reached"),
                            rs.getInt("won") != 0,
                            rs.getString("played_at")));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Ошибка выборки таблицы рекордов", e);
        }
        return result;
    }

    @Override
    public void close() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException ignored) {
            // соединение закрывается при завершении приложения
        }
    }
}
