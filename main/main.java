package main;

import ui.mainFrame;
import javax.swing.SwingUtilities;

public class main {
    public static void main(String[] args) {
        // Garante que a GUI seja criada na Thread de Eventos do Swing
        SwingUtilities.invokeLater(() -> {
            mainFrame frame = new mainFrame();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}