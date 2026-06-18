package com.millionaire.ui;

import com.millionaire.game.Prizes;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

/**
 * Панель денежной лестницы: 15 уровней сверху вниз (3 000 000 — вверху).
 * Подсвечивает текущий уровень и помечает выбранную несгораемую сумму.
 */
public class PrizeLadderPanel extends JPanel {

    private final JLabel[] rows = new JLabel[Prizes.LEVELS];
    private int currentLevel = 0;     // 1..15, 0 — игра не идёт
    private long guaranteedAmount = -1;

    public PrizeLadderPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(new Color(10, 20, 60));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setPreferredSize(new Dimension(220, 0));

        for (int level = Prizes.LEVELS; level >= 1; level--) {
            JLabel row = new JLabel();
            row.setOpaque(true);
            row.setHorizontalAlignment(SwingConstants.CENTER);
            row.setAlignmentX(Component.CENTER_ALIGNMENT);
            row.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            row.setFont(new Font("SansSerif", Font.BOLD, 14));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
            rows[level - 1] = row;
            add(row);
        }
        refresh();
    }

    public void setState(int currentLevel, long guaranteedAmount) {
        this.currentLevel = currentLevel;
        this.guaranteedAmount = guaranteedAmount;
        refresh();
    }

    private void refresh() {
        for (int level = 1; level <= Prizes.LEVELS; level++) {
            JLabel row = rows[level - 1];
            long amount = Prizes.prizeForLevel(level);
            row.setText(level + ".  " + Prizes.format(amount));

            boolean isCurrent = level == currentLevel;
            boolean isGuaranteed = amount == guaranteedAmount;

            if (isCurrent) {
                row.setBackground(new Color(240, 190, 40));
                row.setForeground(Color.BLACK);
            } else if (isGuaranteed) {
                row.setBackground(new Color(40, 120, 220));
                row.setForeground(Color.WHITE);
            } else {
                row.setBackground(new Color(10, 20, 60));
                row.setForeground(new Color(230, 230, 230));
            }
        }
    }
}
