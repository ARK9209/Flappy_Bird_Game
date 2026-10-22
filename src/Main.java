import javax.swing.*;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Flappy Bird - Sky Sprint");
            GamePanel gamePanel = new GamePanel();
            frame.setContentPane(gamePanel);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            // Focus can only be granted after the window becomes displayable.
            SwingUtilities.invokeLater(gamePanel::requestFocusInWindow);
        });
    }
}  
