package Settings;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.Properties;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.stage.DirectoryChooser;

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
        // JavaFX 초기화 (처음 한 번만 실행됨)
        initializeJavaFX();
        
        // JavaFX Application Thread에서 DirectoryChooser 실행
        Platform.runLater(() -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("폴더 선택");
            
            // 현재 경로가 있으면 초기 디렉토리로 설정
            if (path != null && !path.isEmpty()) {
                java.io.File currentDir = new java.io.File(path);
                if (currentDir.exists() && currentDir.isDirectory()) {
                    directoryChooser.setInitialDirectory(currentDir);
                }
            }
            
            java.io.File selectedDir = directoryChooser.showDialog(null);
            
            if (selectedDir != null) {
                // Swing EDT에서 UI 업데이트 실행
                SwingUtilities.invokeLater(() -> {
                    path = selectedDir.getAbsolutePath();
                    saveConfig();
                    // 경로 변경 시 콜백 호출
                    if (onPathChanged != null) {
                        onPathChanged.run();
                    }
                    // pathLabel 업데이트를 위해 다시 호출될 수 있도록
                    updatePathLabel();
                });
            }
        });
    }
    
    private static boolean javaFXInitialized = false;
    
    private void initializeJavaFX() {
        if (!javaFXInitialized) {
            // JavaFX 모듈 경고 억제
            System.setProperty("javafx.platform.traceShutdown", "false");
            System.setProperty("prism.verbose", "false");
            
            // JFXPanel을 생성해서 JavaFX 런타임 초기화
            new JFXPanel();
            javaFXInitialized = true;
        }
    }
    
    private void updatePathLabel() {
        // generalPanel에서 pathLabel을 찾아서 업데이트
        Component[] components = generalPanel.getComponents();
        for (Component comp : components) {
            if (comp instanceof JLabel) {
                JLabel label = (JLabel) comp;
                label.setText(path != null ? path : "기본 경로: servers");
                break;
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
