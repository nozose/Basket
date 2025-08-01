package Settings;

import javax.swing.*;
import java.awt.*;

public class utils {
    public static JButton createTextButton(Integer width, Integer height, String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(width, height));
        return button;
    }
}
