package com.millionaire.game;

import com.millionaire.db.Database;
import com.millionaire.model.Question;

/**
 * Источник вопросов из базы данных SQLite.
 */
public class DbQuestionProvider implements QuestionProvider {

    private final Database db;

    public DbQuestionProvider(Database db) {
        this.db = db;
    }

    @Override
    public Question getQuestion(int level) {
        Question q = db.getRandomQuestion(level);
        if (q == null) {
            throw new IllegalStateException("В базе данных нет вопросов уровня " + level);
        }
        return q;
    }

    @Override
    public String name() {
        return "База данных SQLite";
    }
}
