package com.tictactoe.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Model representing the complete state of a Tic-Tac-Toe game.
 * Holds the 3x3 board matrix, turn information, scores, winning lines, and move logs.
 */
public class GameState {

    public enum GameStatus {
        IN_PROGRESS,
        X_WON,
        O_WON,
        DRAW
    }

    private char[][] board; // 3x3 matrix representing the game grid (' ' for empty, 'X', or 'O')
    private char currentPlayer; // 'X' or 'O'
    private String playerXName;
    private String playerOName;
    private GameStatus status;
    private int roundNumber;
    private int scoreX;
    private int scoreO;
    private int draws;
    private int[][] winningLine; // null or array of 3 coordinates: [[r0, c0], [r1, c1], [r2, c2]]
    private List<String> moveHistory;
    private String gameMode; // "PVP", "AI_EASY", "AI_MEDIUM", "AI_HARD"

    public GameState() {
        this.board = new char[3][3];
        this.currentPlayer = 'X';
        this.playerXName = "Player X";
        this.playerOName = "Player O";
        this.status = GameStatus.IN_PROGRESS;
        this.roundNumber = 1;
        this.scoreX = 0;
        this.scoreO = 0;
        this.draws = 0;
        this.winningLine = null;
        this.moveHistory = new ArrayList<>();
        this.gameMode = "PVP";
        clearBoard();
    }

    /**
     * Initializes or clears the 3x3 board using matrix loops.
     */
    public void clearBoard() {
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                board[r][c] = ' ';
            }
        }
        winningLine = null;
        status = GameStatus.IN_PROGRESS;
        currentPlayer = (roundNumber % 2 == 1) ? 'X' : 'O'; // Alternate starting player each round
        moveHistory.clear();
    }

    public char[][] getBoard() {
        return board;
    }

    public void setBoard(char[][] board) {
        this.board = board;
    }

    public char getCurrentPlayer() {
        return currentPlayer;
    }

    public void setCurrentPlayer(char currentPlayer) {
        this.currentPlayer = currentPlayer;
    }

    public String getPlayerXName() {
        return playerXName;
    }

    public void setPlayerXName(String playerXName) {
        this.playerXName = (playerXName != null && !playerXName.trim().isEmpty()) ? playerXName.trim() : "Player X";
    }

    public String getPlayerOName() {
        return playerOName;
    }

    public void setPlayerOName(String playerOName) {
        this.playerOName = (playerOName != null && !playerOName.trim().isEmpty()) ? playerOName.trim() : "Player O";
    }

    public GameStatus getStatus() {
        return status;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public void setRoundNumber(int roundNumber) {
        this.roundNumber = roundNumber;
    }

    public int getScoreX() {
        return scoreX;
    }

    public void setScoreX(int scoreX) {
        this.scoreX = scoreX;
    }

    public int getScoreO() {
        return scoreO;
    }

    public void setScoreO(int scoreO) {
        this.scoreO = scoreO;
    }

    public int getDraws() {
        return draws;
    }

    public void setDraws(int draws) {
        this.draws = draws;
    }

    public int[][] getWinningLine() {
        return winningLine;
    }

    public void setWinningLine(int[][] winningLine) {
        this.winningLine = winningLine;
    }

    public List<String> getMoveHistory() {
        return moveHistory;
    }

    public void addMoveToHistory(String moveDescription) {
        this.moveHistory.add(moveDescription);
    }

    public String getGameMode() {
        return gameMode;
    }

    public void setGameMode(String gameMode) {
        this.gameMode = gameMode;
    }

    /**
     * Serializes GameState to clean JSON without requiring external libraries.
     */
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"currentPlayer\":\"").append(currentPlayer).append("\",");
        sb.append("\"playerXName\":\"").append(escapeJson(playerXName)).append("\",");
        sb.append("\"playerOName\":\"").append(escapeJson(playerOName)).append("\",");
        sb.append("\"status\":\"").append(status.name()).append("\",");
        sb.append("\"roundNumber\":").append(roundNumber).append(",");
        sb.append("\"scoreX\":").append(scoreX).append(",");
        sb.append("\"scoreO\":").append(scoreO).append(",");
        sb.append("\"draws\":").append(draws).append(",");
        sb.append("\"gameMode\":\"").append(escapeJson(gameMode)).append("\",");

        // Board 2D matrix
        sb.append("\"board\":[");
        for (int r = 0; r < 3; r++) {
            sb.append("[");
            for (int c = 0; c < 3; c++) {
                sb.append("\"").append(board[r][c]).append("\"");
                if (c < 2) sb.append(",");
            }
            sb.append("]");
            if (r < 2) sb.append(",");
        }
        sb.append("],");

        // Winning line
        sb.append("\"winningLine\":");
        if (winningLine == null) {
            sb.append("null,");
        } else {
            sb.append("[");
            for (int i = 0; i < winningLine.length; i++) {
                sb.append("[").append(winningLine[i][0]).append(",").append(winningLine[i][1]).append("]");
                if (i < winningLine.length - 1) sb.append(",");
            }
            sb.append("],");
        }

        // Move history
        sb.append("\"moveHistory\":[");
        for (int i = 0; i < moveHistory.size(); i++) {
            sb.append("\"").append(escapeJson(moveHistory.get(i))).append("\"");
            if (i < moveHistory.size() - 1) sb.append(",");
        }
        sb.append("]");

        sb.append("}");
        return sb.toString();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
