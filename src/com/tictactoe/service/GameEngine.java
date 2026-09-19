package com.tictactoe.service;

import com.tictactoe.model.GameState;
import com.tictactoe.model.GameState.GameStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Core Game Engine implementing Tic-Tac-Toe mechanics using:
 * - 2D Arrays / Matrices (char[3][3])
 * - Loops (iterating rows, columns, and diagonals)
 * - Conditional Statements (validating moves, checking win patterns, draw detection)
 */
public class GameEngine {

    private final GameState state;
    private final Random random;

    public GameEngine() {
        this.state = new GameState();
        this.random = new Random();
    }

    public GameState getState() {
        return state;
    }

    /**
     * Executes a player move at specified matrix coordinates [row][col].
     * Uses conditional statements to validate cell availability and game status.
     */
    public synchronized boolean makeMove(int row, int col) {
        // 1. Conditional check: Verify game is currently in progress
        if (state.getStatus() != GameStatus.IN_PROGRESS) {
            return false;
        }

        // 2. Conditional check: Boundary validation (0 to 2)
        if (row < 0 || row >= 3 || col < 0 || col >= 3) {
            return false;
        }

        char[][] board = state.getBoard();

        // 3. Conditional check: Cell must be empty (' ')
        if (board[row][col] != ' ') {
            return false;
        }

        char player = state.getCurrentPlayer();
        String playerName = (player == 'X') ? state.getPlayerXName() : state.getPlayerOName();

        // Update the 2D matrix
        board[row][col] = player;
        state.addMoveToHistory(playerName + " (" + player + ") placed at [" + (row + 1) + ", " + (col + 1) + "]");

        // Check for win condition using matrix loops and conditionals
        int[][] winCoordinates = checkWin(board, player);
        if (winCoordinates != null) {
            state.setWinningLine(winCoordinates);
            if (player == 'X') {
                state.setStatus(GameStatus.X_WON);
                state.setScoreX(state.getScoreX() + 1);
            } else {
                state.setStatus(GameStatus.O_WON);
                state.setScoreO(state.getScoreO() + 1);
            }
            return true;
        }

        // Check for draw condition using loops across the matrix
        if (isBoardFull(board)) {
            state.setStatus(GameStatus.DRAW);
            state.setDraws(state.getDraws() + 1);
            return true;
        }

        // Switch active player
        state.setCurrentPlayer((player == 'X') ? 'O' : 'X');

        // If playing against AI and it is now Player O's turn, execute AI move
        if (state.getGameMode().startsWith("AI_") && state.getCurrentPlayer() == 'O' && state.getStatus() == GameStatus.IN_PROGRESS) {
            makeAiMove();
        }

        return true;
    }

    /**
     * Checks if the specified player has won the game.
     * Evaluates 3 rows, 3 columns, and 2 diagonals using loops and conditional statements.
     * Returns a 3x2 matrix of winning cell coordinates, or null if no win.
     */
    public int[][] checkWin(char[][] b, char p) {
        // 1. Loop through all 3 rows
        for (int r = 0; r < 3; r++) {
            if (b[r][0] == p && b[r][1] == p && b[r][2] == p) {
                return new int[][] { { r, 0 }, { r, 1 }, { r, 2 } };
            }
        }

        // 2. Loop through all 3 columns
        for (int c = 0; c < 3; c++) {
            if (b[0][c] == p && b[1][c] == p && b[2][c] == p) {
                return new int[][] { { 0, c }, { 1, c }, { 2, c } };
            }
        }

        // 3. Conditional check: Main diagonal (top-left to bottom-right)
        if (b[0][0] == p && b[1][1] == p && b[2][2] == p) {
            return new int[][] { { 0, 0 }, { 1, 1 }, { 2, 2 } };
        }

        // 4. Conditional check: Anti-diagonal (top-right to bottom-left)
        if (b[0][2] == p && b[1][1] == p && b[2][0] == p) {
            return new int[][] { { 0, 2 }, { 1, 1 }, { 2, 0 } };
        }

        return null; // No win detected
    }

    /**
     * Checks if the matrix board is completely filled.
     * Uses nested loops to check every cell in the 3x3 array.
     */
    public boolean isBoardFull(char[][] b) {
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (b[r][c] == ' ') {
                    return false; // Found an empty cell
                }
            }
        }
        return true; // No empty cells left
    }

    /**
     * Resets the board matrix for the next round while preserving overall match scores.
     */
    public synchronized void resetRound() {
        state.setRoundNumber(state.getRoundNumber() + 1);
        state.clearBoard();
        // If AI mode and AI starts
        if (state.getGameMode().startsWith("AI_") && state.getCurrentPlayer() == 'O') {
            makeAiMove();
        }
    }

    /**
     * Restarts the current round board without advancing the round counter or resetting scores.
     */
    public synchronized void restartRound() {
        state.clearBoard();
        if (state.getGameMode().startsWith("AI_") && state.getCurrentPlayer() == 'O') {
            makeAiMove();
        }
    }

    /**
     * Completely resets the match, resetting scores, round count, and matrix board.
     */
    public synchronized void newGame() {
        state.setRoundNumber(1);
        state.setScoreX(0);
        state.setScoreO(0);
        state.setDraws(0);
        state.clearBoard();
    }

    /**
     * Updates player display names.
     */
    public synchronized void setPlayerNames(String nameX, String nameO) {
        state.setPlayerXName(nameX);
        state.setPlayerOName(nameO);
    }

    /**
     * Sets game mode ("PVP", "AI_EASY", "AI_MEDIUM", "AI_HARD").
     */
    public synchronized void setGameMode(String mode) {
        state.setGameMode(mode);
        if (mode.startsWith("AI_")) {
            state.setPlayerOName("Computer (" + mode.replace("AI_", "") + ")");
        } else {
            state.setPlayerOName("Player O");
        }
    }

    /**
     * AI Move Generator supporting Easy (Random), Medium (Heuristic Blocks/Wins),
     * and Hard (Minimax algorithm).
     */
    private void makeAiMove() {
        char[][] board = state.getBoard();
        String mode = state.getGameMode();

        List<int[]> emptyCells = new ArrayList<>();
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (board[r][c] == ' ') {
                    emptyCells.add(new int[] { r, c });
                }
            }
        }

        if (emptyCells.isEmpty()) return;

        int chosenRow = -1;
        int chosenCol = -1;

        if ("AI_EASY".equals(mode)) {
            // Random move
            int[] cell = emptyCells.get(random.nextInt(emptyCells.size()));
            chosenRow = cell[0];
            chosenCol = cell[1];
        } else if ("AI_MEDIUM".equals(mode)) {
            // 1. Can AI win immediately?
            int[] winMove = findWinningMove(board, 'O');
            if (winMove != null) {
                chosenRow = winMove[0];
                chosenCol = winMove[1];
            } else {
                // 2. Can player X win immediately? Block them!
                int[] blockMove = findWinningMove(board, 'X');
                if (blockMove != null) {
                    chosenRow = blockMove[0];
                    chosenCol = blockMove[1];
                } else if (board[1][1] == ' ') {
                    chosenRow = 1;
                    chosenCol = 1; // Take center
                } else {
                    int[] cell = emptyCells.get(random.nextInt(emptyCells.size()));
                    chosenRow = cell[0];
                    chosenCol = cell[1];
                }
            }
        } else {
            // AI_HARD: Unbeatable Minimax
            int bestScore = Integer.MIN_VALUE;
            for (int[] cell : emptyCells) {
                int r = cell[0];
                int c = cell[1];
                board[r][c] = 'O';
                int score = minimax(board, 0, false);
                board[r][c] = ' '; // backtrack
                if (score > bestScore) {
                    bestScore = score;
                    chosenRow = r;
                    chosenCol = c;
                }
            }
        }

        if (chosenRow != -1 && chosenCol != -1) {
            board[chosenRow][chosenCol] = 'O';
            state.addMoveToHistory(state.getPlayerOName() + " placed at [" + (chosenRow + 1) + ", " + (chosenCol + 1) + "]");

            int[][] winCoordinates = checkWin(board, 'O');
            if (winCoordinates != null) {
                state.setWinningLine(winCoordinates);
                state.setStatus(GameStatus.O_WON);
                state.setScoreO(state.getScoreO() + 1);
            } else if (isBoardFull(board)) {
                state.setStatus(GameStatus.DRAW);
                state.setDraws(state.getDraws() + 1);
            } else {
                state.setCurrentPlayer('X');
            }
        }
    }

    private int[] findWinningMove(char[][] b, char player) {
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (b[r][c] == ' ') {
                    b[r][c] = player;
                    boolean wins = (checkWin(b, player) != null);
                    b[r][c] = ' '; // backtrack
                    if (wins) return new int[] { r, c };
                }
            }
        }
        return null;
    }

    private int minimax(char[][] b, int depth, boolean isMaximizing) {
        if (checkWin(b, 'O') != null) return 10 - depth;
        if (checkWin(b, 'X') != null) return depth - 10;
        if (isBoardFull(b)) return 0;

        if (isMaximizing) {
            int maxEval = Integer.MIN_VALUE;
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    if (b[r][c] == ' ') {
                        b[r][c] = 'O';
                        int eval = minimax(b, depth + 1, false);
                        b[r][c] = ' ';
                        maxEval = Math.max(maxEval, eval);
                    }
                }
            }
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    if (b[r][c] == ' ') {
                        b[r][c] = 'X';
                        int eval = minimax(b, depth + 1, true);
                        b[r][c] = ' ';
                        minEval = Math.min(minEval, eval);
                    }
                }
            }
            return minEval;
        }
    }
}
