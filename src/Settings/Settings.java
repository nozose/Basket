package Settings;

import javax.swing.*;
import java.awt.*;

public class Settings {
    public JFrame showSettings() {
        JFrame settingFrame = new JFrame("설정");
        settingFrame.setSize(new Dimension(700, 600));

        settingFrame.setLocationRelativeTo(null);
        settingFrame.setResizable(false);
        settingFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        settingFrame.setVisible(true);
        return settingFrame;
    }
}
