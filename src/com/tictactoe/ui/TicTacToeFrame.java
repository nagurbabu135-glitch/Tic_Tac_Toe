package com.tictactoe.ui;

import com.tictactoe.model.GameState;
import com.tictactoe.model.GameState.GameStatus;
import com.tictactoe.service.GameEngine;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URI;
import java.util.List;

/**
 * Desktop GUI for Tic-Tac-Toe built with Java Swing and styled with FlatLaf modern dark UI.
 * Features real-time scoreboards, 3x3 interactive matrix buttons, round controls,
 * and quick-launch links to the Web Dashboard.
 */
public class TicTacToeFrame extends JFrame {

    private final GameEngine engine;
    private final int webPort;

    private final JButton[][] cellButtons = new JButton[3][3];
    private JLabel statusLabel;
    private JLabel scoreXLabel;
    private JLabel scoreOLabel;
    private JLabel drawsLabel;
    private JLabel roundLabel;
    private JComboBox<String> modeComboBox;
    private JLabel networkLabel;

    public TicTacToeFrame(GameEngine engine, int webPort) {
        this.engine = engine;
        this.webPort = webPort;

        setTitle("Tic-Tac-Toe - Two-Player & Universal Web Game");
        setSize(580, 750);
        setMinimumSize(new Dimension(500, 680));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        initUI();
        updateBoardDisplay();

        // Timer to periodically poll state in case moves were made via Web/Mobile clients
        Timer syncTimer = new Timer(600, e -> updateBoardDisplay());
        syncTimer.start();
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout(15, 15));
        root.setBackground(new Color(15, 23, 42)); // Slate 900
        root.setBorder(new EmptyBorder(18, 20, 18, 20));

        // 1. Top Header Panel
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("TIC-TAC-TOE PRO");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        titleLabel.setForeground(new Color(56, 189, 248)); // Sky blue
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Two-Player Matrix Game • Play Here or on Any Device");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setForeground(new Color(148, 163, 184)); // Slate 400
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        topPanel.add(titleLabel);
        topPanel.add(Box.createVerticalStrut(4));
        topPanel.add(subtitleLabel);
        topPanel.add(Box.createVerticalStrut(12));

        // Scorecard Cards Grid
        JPanel scorePanel = new JPanel(new GridLayout(1, 4, 10, 0));
        scorePanel.setOpaque(false);
        scorePanel.setMaximumSize(new Dimension(540, 65));

        roundLabel = createScoreCard("ROUND", "1", new Color(168, 85, 247), scorePanel);
        scoreXLabel = createScoreCard("PLAYER X", "0", new Color(56, 189, 248), scorePanel);
        scoreOLabel = createScoreCard("PLAYER O", "0", new Color(244, 63, 94), scorePanel);
        drawsLabel = createScoreCard("DRAWS", "0", new Color(234, 179, 8), scorePanel);

        topPanel.add(scorePanel);
        topPanel.add(Box.createVerticalStrut(12));

        // Status Banner
        statusLabel = new JLabel("Player X's Turn (X)", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setOpaque(true);
        statusLabel.setBackground(new Color(30, 41, 59));
        statusLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(51, 65, 85), 1),
                new EmptyBorder(8, 14, 8, 14)
        ));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel.setMaximumSize(new Dimension(540, 40));
        topPanel.add(statusLabel);

        root.add(topPanel, BorderLayout.NORTH);

        // 2. Center 3x3 Board Grid
        JPanel boardPanel = new JPanel(new GridLayout(3, 3, 10, 10));
        boardPanel.setOpaque(false);
        boardPanel.setBorder(new EmptyBorder(10, 15, 10, 15));

        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                JButton btn = new JButton("");
                btn.setFont(new Font("Segoe UI", Font.BOLD, 48));
                btn.setFocusPainted(false);
                btn.setBackground(new Color(30, 41, 59));
                btn.setForeground(Color.WHITE);
                btn.setBorder(BorderFactory.createLineBorder(new Color(51, 65, 85), 2));
                btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

                final int row = r;
                final int col = c;
                btn.addActionListener((ActionEvent e) -> handleCellClick(row, col));

                cellButtons[r][c] = btn;
                boardPanel.add(btn);
            }
        }
        root.add(boardPanel, BorderLayout.CENTER);

        // 3. Bottom Controls Panel
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setOpaque(false);

        // Game Mode & Buttons row
        JPanel actionsRow1 = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        actionsRow1.setOpaque(false);

        JLabel modeLbl = new JLabel("Mode:");
        modeLbl.setForeground(new Color(203, 213, 225));
        actionsRow1.add(modeLbl);

        modeComboBox = new JComboBox<>(new String[]{
                "2-Player Local (Pass & Play)",
                "vs Computer (Easy)",
                "vs Computer (Medium)",
                "vs Computer (Unbeatable)"
        });
        modeComboBox.setBackground(new Color(30, 41, 59));
        modeComboBox.setForeground(Color.WHITE);
        modeComboBox.addActionListener(e -> {
            int idx = modeComboBox.getSelectedIndex();
            String mode = switch (idx) {
                case 1 -> "AI_EASY";
                case 2 -> "AI_MEDIUM";
                case 3 -> "AI_HARD";
                default -> "PVP";
            };
            engine.setGameMode(mode);
            updateBoardDisplay();
        });
        actionsRow1.add(modeComboBox);

        JButton restartRoundBtn = createStyledButton("Restart Round ↺", new Color(71, 85, 105));
        restartRoundBtn.addActionListener(e -> {
            engine.restartRound();
            updateBoardDisplay();
        });
        actionsRow1.add(restartRoundBtn);

        JButton nextRoundBtn = createStyledButton("Next Round ❯", new Color(37, 99, 235));
        nextRoundBtn.addActionListener(e -> {
            engine.resetRound();
            updateBoardDisplay();
        });
        actionsRow1.add(nextRoundBtn);

        JButton newMatchBtn = createStyledButton("Restart Game ⟲", new Color(180, 83, 9));
        newMatchBtn.addActionListener(e -> {
            engine.newGame();
            updateBoardDisplay();
        });
        actionsRow1.add(newMatchBtn);

        bottomPanel.add(actionsRow1);

        // Web Dashboard Launch Row
        JPanel actionsRow2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        actionsRow2.setOpaque(false);

        JButton openBrowserBtn = createStyledButton("🌐 Open Web Dashboard in Browser", new Color(13, 148, 136));
        openBrowserBtn.addActionListener(e -> openWebDashboard());
        actionsRow2.add(openBrowserBtn);

        bottomPanel.add(actionsRow2);

        // Network connection indicator
        List<String> ips = com.tictactoe.web.TicTacToeWebServer.getNetworkIps();
        String ipText = ips.isEmpty() ? "localhost:" + webPort : ips.get(0) + ":" + webPort;
        networkLabel = new JLabel("Web Server: http://localhost:" + webPort + "  |  Mobile LAN: http://" + ipText, SwingConstants.CENTER);
        networkLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        networkLabel.setForeground(new Color(100, 116, 139));
        networkLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        bottomPanel.add(networkLabel);

        root.add(bottomPanel, BorderLayout.SOUTH);
        setContentPane(root);
    }

    private JLabel createScoreCard(String title, String initialVal, Color accentColor, JPanel parent) {
        JPanel card = new JPanel(new BorderLayout(2, 2));
        card.setOpaque(true);
        card.setBackground(new Color(30, 41, 59));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accentColor, 1),
                new EmptyBorder(6, 6, 6, 6)
        ));

        JLabel titleLbl = new JLabel(title, SwingConstants.CENTER);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        titleLbl.setForeground(accentColor);

        JLabel valLbl = new JLabel(initialVal, SwingConstants.CENTER);
        valLbl.setFont(new Font("Segoe UI", Font.BOLD, 18));
        valLbl.setForeground(Color.WHITE);

        card.add(titleLbl, BorderLayout.NORTH);
        card.add(valLbl, BorderLayout.CENTER);
        parent.add(card);
        return valLbl;
    }

    private JButton createStyledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(7, 12, 7, 12));
        return btn;
    }

    private void handleCellClick(int row, int col) {
        boolean moved = engine.makeMove(row, col);
        if (moved) {
            updateBoardDisplay();
        }
    }

    private void updateBoardDisplay() {
        GameState state = engine.getState();
        char[][] board = state.getBoard();
        int[][] winLine = state.getWinningLine();

        // Update score indicators
        roundLabel.setText(String.valueOf(state.getRoundNumber()));
        scoreXLabel.setText(String.valueOf(state.getScoreX()));
        scoreOLabel.setText(String.valueOf(state.getScoreO()));
        drawsLabel.setText(String.valueOf(state.getDraws()));

        // Update 3x3 Matrix Buttons
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                char symbol = board[r][c];
                JButton btn = cellButtons[r][c];
                btn.setText(symbol == ' ' ? "" : String.valueOf(symbol));

                // Symbol coloring
                if (symbol == 'X') {
                    btn.setForeground(new Color(56, 189, 248)); // Neon Cyan
                } else if (symbol == 'O') {
                    btn.setForeground(new Color(244, 63, 94));  // Neon Pink/Rose
                } else {
                    btn.setForeground(Color.WHITE);
                }

                // Highlight winning line if round is finished
                boolean isWinningCell = false;
                if (winLine != null) {
                    for (int[] coord : winLine) {
                        if (coord[0] == r && coord[1] == c) {
                            isWinningCell = true;
                            break;
                        }
                    }
                }

                if (isWinningCell) {
                    btn.setBackground(new Color(16, 185, 129)); // Emerald Green highlight
                    btn.setBorder(BorderFactory.createLineBorder(new Color(52, 211, 153), 3));
                } else {
                    btn.setBackground(new Color(30, 41, 59));
                    btn.setBorder(BorderFactory.createLineBorder(new Color(51, 65, 85), 2));
                }
            }
        }

        // Update Status Banner
        GameStatus status = state.getStatus();
        if (status == GameStatus.IN_PROGRESS) {
            char curr = state.getCurrentPlayer();
            String name = (curr == 'X') ? state.getPlayerXName() : state.getPlayerOName();
            statusLabel.setText(name + "'s Turn (" + curr + ")");
            statusLabel.setBackground(new Color(30, 41, 59));
            statusLabel.setForeground((curr == 'X') ? new Color(56, 189, 248) : new Color(244, 63, 94));
        } else if (status == GameStatus.X_WON) {
            statusLabel.setText("🏆 " + state.getPlayerXName() + " WON ROUND " + state.getRoundNumber() + "!");
            statusLabel.setBackground(new Color(6, 78, 59)); // Dark Green
            statusLabel.setForeground(new Color(52, 211, 153));
        } else if (status == GameStatus.O_WON) {
            statusLabel.setText("🏆 " + state.getPlayerOName() + " WON ROUND " + state.getRoundNumber() + "!");
            statusLabel.setBackground(new Color(136, 19, 55)); // Dark Rose
            statusLabel.setForeground(new Color(251, 113, 133));
        } else if (status == GameStatus.DRAW) {
            statusLabel.setText("🤝 IT'S A DRAW! Round " + state.getRoundNumber() + " Tied");
            statusLabel.setBackground(new Color(113, 63, 18)); // Dark Amber
            statusLabel.setForeground(new Color(253, 224, 71));
        }
    }

    private void openWebDashboard() {
        try {
            Desktop.getDesktop().browse(URI.create("http://localhost:" + webPort));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Open browser and navigate to: http://localhost:" + webPort,
                    "Web Dashboard", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
