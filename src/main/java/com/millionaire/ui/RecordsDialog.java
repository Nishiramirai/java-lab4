package com.millionaire.ui;

import com.millionaire.db.Database;
import com.millionaire.game.Prizes;
import com.millionaire.model.ScoreRecord;

import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.util.List;

/**
 * Диалог «Таблица рекордов»: выводит TOP-10 игроков по размеру выигрыша.
 */
public class RecordsDialog extends JDialog {

    public RecordsDialog(Frame owner, Database db) {
        super(owner, "Таблица рекордов — TOP 10", true);

        String[] columns = {"Место", "Игрок", "Выигрыш", "Уровень", "Победа", "Дата"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        List<ScoreRecord> top = db.getTopRecords(10);
        int place = 1;
        for (ScoreRecord r : top) {
            model.addRow(new Object[]{
                    place++,
                    r.getPlayerName(),
                    Prizes.format(r.getPrize()),
                    r.getLevelReached() + " / " + Prizes.LEVELS,
                    r.isWon() ? "Да" : "—",
                    r.getPlayedAt()
            });
        }

        JTable table = new JTable(model);
        table.setRowHeight(24);
        table.getTableHeader().setReorderingAllowed(false);

        add(new JScrollPane(table), BorderLayout.CENTER);
        setSize(620, 320);
        setLocationRelativeTo(owner);
    }
}
