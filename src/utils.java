import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class utils {

    public JButton createTextButton(Integer width, Integer height, String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(width, height));
        return button;
    }

    public JButton createImageButton(Integer width, Integer height, String path) {
        JButton button = new JButton(resizeIcon(loadIcon(path), width, height));
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(width, height));
        return button;
    }








    // 유틸리티 필요 함수들
    public static ImageIcon resizeIcon(ImageIcon icon, int width, int height) {
        int size = Math.min(width, height);
        Image img = icon.getImage();
        Image resized = img.getScaledInstance(size, size, Image.SCALE_SMOOTH);
        return new ImageIcon(resized);
    }

    public static ImageIcon loadIcon(String path) {
        try {
            return new ImageIcon(Objects.requireNonNull(Basket.class.getResource(path)));
        } catch (Exception e) {
            System.err.println("이미지 로드 실패: " + path);
            return new ImageIcon();
        }
    }
    // 유틸리티 필요 함수들
}
