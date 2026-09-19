package com.tictactoe;

import com.tictactoe.service.GameEngine;
import com.tictactoe.ui.TicTacToeFrame;
import com.tictactoe.web.TicTacToeWebServer;

import javax.swing.*;
import java.awt.Desktop;
import java.net.URI;
import java.util.List;

/**
 * Main Application Entry Point for Tic-Tac-Toe Pro.
 * Starts the embedded Web Server (0.0.0.0:8096), launches the modern Swing Desktop GUI,
 * and enables access from any device, anytime, in any browser.
 */
public class Main {

    private static final int PORT = 8096;

    public static void main(String[] args) {
        System.out.println("=========================================================");
        System.out.println("        TIC-TAC-TOE PRO • UNIVERSAL GAME PLATFORM       ");
        System.out.println("   Two-Player Matrix Engine & Multi-Round Tournament   ");
        System.out.println("=========================================================");

        GameEngine engine = new GameEngine();

        // 1. Start Embedded HTTP Web Server
        try {
            TicTacToeWebServer webServer = new TicTacToeWebServer(PORT, engine);
            webServer.start();
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to start Web Server: " + e.getMessage());
        }

        // 2. Try applying FlatLaf dark look and feel
        try {
            UIManager.setLookAndFeel("com.formdev.flatlaf.FlatDarkLaf");
        } catch (Exception ignored) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored2) {}
        }

        // 3. Launch Desktop GUI if display environment is available
        if (!java.awt.GraphicsEnvironment.isHeadless()) {
            SwingUtilities.invokeLater(() -> {
                TicTacToeFrame frame = new TicTacToeFrame(engine, PORT);
                frame.setVisible(true);
            });

            // Auto-open Web Dashboard in browser
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(URI.create("http://localhost:" + PORT));
                }
            } catch (Exception ignored) {}
        } else {
            System.out.println("[INFO] Running in headless mode. Web Dashboard available at http://localhost:" + PORT);
        }

        List<String> ips = TicTacToeWebServer.getNetworkIps();
        System.out.println("\n[READY] Game is accessible across devices:");
        System.out.println("  • Local Browser:  http://localhost:" + PORT);
        for (String ip : ips) {
            System.out.println("  • Mobile / LAN:   http://" + ip + ":" + PORT);
        }
        System.out.println("=========================================================\n");
    }
}
