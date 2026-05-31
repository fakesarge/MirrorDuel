package mirrorduel;

import javax.swing.SwingUtilities;

//entry point of the whole application
//SwingUtilities.invokeLater makes sure the window is built on the correct thread
//java swing requires all window code to run on the Event Dispatch Thread otherwise you get random bugs
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            //create the main window and make it visible
            GameWindow window = new GameWindow();
            window.setVisible(true);
        });
    }
}
