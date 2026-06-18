package com.millionaire.game;

import com.millionaire.model.Question;

/**
 * Источник вопросов для игры. Абстракция позволяет получать вопросы из базы
 * данных SQLite, из текстового файла либо генерировать их средствами ИИ, не меняя
 * игровую логику.
 */
public interface QuestionProvider {

    /**
     * @param level уровень сложности 1..15
     * @return вопрос указанного уровня
     * @throws Exception если вопрос получить не удалось
     */
    Question getQuestion(int level) throws Exception;

    /** Короткое название источника для отображения в интерфейсе. */
    String name();
}
