package Settings;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.Properties;

public class Settings {
    private static final String CONFIG_FILE = "config.properties";

    JFrame settingFrame = new JFrame("설정");
    JPanel generalPanel = new JPanel();

    String path;
    private Runnable onPathChanged;

    // 1 = 일반 | 2 = 미정
    Integer current = 1;

    public JFrame showSettings() {
        return showSettings(null);
    }

    public JFrame showSettings(Runnable onPathChanged) {
        this.onPathChanged = onPathChanged;
        JButton general = utils.createTextButton(70, 40, "일반");

        settingFrame.setSize(new Dimension(700, 600));
        settingFrame.setLocationRelativeTo(null);
        settingFrame.setResizable(false);
        settingFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JButton selectFolderBtn = new JButton("경로 선택");
        JLabel pathLabel = new JLabel(path != null ? path : "기본 경로: servers");
        selectFolderBtn.addActionListener(e -> {
            selectAndSaveFolderPath();
            pathLabel.setText(path != null ? path : "기본 경로: servers");
        });
        generalPanel.add(selectFolderBtn);
        generalPanel.add(pathLabel);

        settingFrame.add(generalPanel);

        JPanel nav = new JPanel(new FlowLayout(FlowLayout.LEFT));
        nav.setBorder(BorderFactory.createLineBorder(Color.black));
        settingFrame.add(nav, BorderLayout.WEST);

        nav.add(general);
        general.addActionListener(e -> one());

        settingFrame.setVisible(true);
        return settingFrame;
    }

    public void one() {
        if (current == 1) return;

        JButton selectFolderBtn = new JButton("경로 선택");
        selectFolderBtn.addActionListener(e -> selectAndSaveFolderPath());

        generalPanel.setBackground(Color.RED);
        settingFrame.add(generalPanel);
    }

    public void selectAndSaveFolderPath() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int result = chooser.showOpenDialog(settingFrame);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedDir = chooser.getSelectedFile();
            path = selectedDir.getAbsolutePath();
            saveConfig();
            // 경로 변경 시 콜백 호출
            if (onPathChanged != null) {
                onPathChanged.run();
            }
        }
    }

    public String getPathOrDefault(String defaultPath) {
        if (path == null || path.isEmpty()) {
            return defaultPath;
        }
        return path;
    }

    public void loadConfig() {
        try (InputStream input = new FileInputStream(CONFIG_FILE)) {
            Properties prop = new Properties();
            prop.load(input);
            path = prop.getProperty("path");
        } catch (IOException ex) {
            // Config file doesn't exist or can't be read, use default
        }
    }

    public void saveConfig() {
        try (OutputStream output = new FileOutputStream(CONFIG_FILE)) {
            Properties prop = new Properties();
            if (path != null) {
                prop.setProperty("path", path);
            }
            prop.store(output, null);
        } catch (IOException io) {
            io.printStackTrace();
        }
    }

    public Settings() {
        loadConfig();
    }

}
