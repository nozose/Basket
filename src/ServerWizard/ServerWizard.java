package ServerWizard;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

public class ServerWizard {
    private JPanel setversion;
    private JPanel setNamePanel;
    private JPanel launcherPanel;
    private JPanel eulaPanel;
    private JTextField nameField;
    private JButton nextButton;
    private JLabel warningLabel;

    // 저장할 임시 데이터
    private String selectedVersion = null;
    private String serverName = "";
    private String selectedLauncher = "Paper";
    private boolean agreedEula = false;

    private ServerCreationCallback callback;

    public interface ServerCreationCallback {
        void onServerCreated(String name, String version, String launcher, Boolean eula);
    }

    public void showWizard(ServerCreationCallback callback) {
        this.callback = callback;
        final int[] current = {1};

        JFrame wizardFrame = new JFrame("서버 추가");
        wizardFrame.setSize(540, 360);
        wizardFrame.setLocationRelativeTo(null);
        wizardFrame.setResizable(false);
        wizardFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // 창이 닫힐 때 부모의 addButton을 다시 활성화하기 위해 콜백 호출
        wizardFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                // 부모 창의 addButton 재활성화는 부모에서 처리하도록 함
            }
        });

        JLabel currentStepLabel = new JLabel("버전");
        currentStepLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        labelPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        labelPanel.add(currentStepLabel);
        wizardFrame.add(labelPanel, BorderLayout.NORTH);

        JPanel navigation = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        JButton previousButton = utils.createTextButton(80, 35, "이전");
        nextButton = utils.createTextButton(80, 35, "다음");
        previousButton.setEnabled(false);

        navigation.add(previousButton);
        navigation.add(nextButton);
        wizardFrame.add(navigation, BorderLayout.SOUTH);

        // 각 단계 패널 초기화
        initializePanels();
        wizardFrame.add(setversion, BorderLayout.CENTER);

        // 버튼 이벤트 리스너
        nextButton.addActionListener(e -> {
            if (nextButton.getText().equals("완료")) {
                callback.onServerCreated(serverName, selectedVersion, selectedLauncher, agreedEula);
                wizardFrame.dispose();
                return;
            }
            current[0]++;
            updateStepUI(current[0], currentStepLabel, previousButton, nextButton, wizardFrame);
        });

        previousButton.addActionListener(e -> {
            if (current[0] > 1) {
                current[0]--;
                updateStepUI(current[0], currentStepLabel, previousButton, nextButton, wizardFrame);
            }
        });

        wizardFrame.setVisible(true);
    }

    private void initializePanels() {
        setversion = createVersionPanel();
        setNamePanel = createNamePanel();
        launcherPanel = createLauncherSelectionPanel();
        eulaPanel = createEulaPanel();
    }

    private JPanel createVersionPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel loadingLabel = new JLabel("버전 목록을 불러오는 중...");
        loadingLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        loadingLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));

        panel.add(Box.createVerticalGlue());
        panel.add(loadingLabel);
        panel.add(Box.createVerticalGlue());

        new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                return utils.getMinecraftVersions();
            }

            @Override
            protected void done() {
                try {
                    List<String> versions = get();
                    panel.removeAll();

                    JLabel label = new JLabel("마인크래프트 버전:");
                    label.setAlignmentX(Component.CENTER_ALIGNMENT);
                    label.setFont(new Font("SansSerif", Font.BOLD, 16));

                    JComboBox<String> versionDropdown = new JComboBox<>(versions.toArray(new String[0]));
                    versionDropdown.setAlignmentX(Component.CENTER_ALIGNMENT);
                    versionDropdown.setMaximumSize(new Dimension(200, 30));
                    versionDropdown.setFont(new Font("SansSerif", Font.PLAIN, 14));
                    versionDropdown.setSelectedIndex(0);
                    selectedVersion = (String) versionDropdown.getSelectedItem();

                    versionDropdown.addActionListener(e -> selectedVersion = (String) versionDropdown.getSelectedItem());

                    panel.add(Box.createVerticalGlue());
                    panel.add(label);
                    panel.add(Box.createRigidArea(new Dimension(0, 10)));
                    panel.add(versionDropdown);
                    panel.add(Box.createVerticalGlue());

                    panel.revalidate();
                    panel.repaint();
                } catch (Exception ex) {
                    panel.removeAll();
                    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
                    JLabel errorLabel = new JLabel("버전 정보를 불러오는 데 실패했습니다.");
                    errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                    panel.add(Box.createVerticalGlue());
                    panel.add(errorLabel);
                    panel.add(Box.createVerticalGlue());
                    panel.revalidate();
                    panel.repaint();
                    ex.printStackTrace();
                }
            }
        }.execute();

        return panel;
    }

    private JPanel createNamePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel label = new JLabel("서버 이름 입력:");
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setFont(new Font("SansSerif", Font.BOLD, 16));

        nameField = new JTextField();
        nameField.setMaximumSize(new Dimension(200, 30));
        nameField.setAlignmentX(Component.CENTER_ALIGNMENT);
        nameField.setFont(new Font("SansSerif", Font.PLAIN, 14));

        warningLabel = new JLabel("");
        warningLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        warningLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        warningLabel.setForeground(Color.RED);

        nameField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { validateName(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { validateName(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { validateName(); }

            public void validateName() {
                serverName = nameField.getText().trim();
                Set<String> existingServers = utils.getExistingServerNames();

                if (serverName.isEmpty()) {
                    warningLabel.setText("");
                    nextButton.setEnabled(false);
                } else if (existingServers.contains(serverName)) {
                    warningLabel.setText("이미 존재하는 서버 이름입니다.");
                    nextButton.setEnabled(false);
                } else {
                    warningLabel.setText("");
                    nextButton.setEnabled(true);
                }
            }
        });

        panel.add(Box.createVerticalGlue());
        panel.add(label);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(nameField);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(warningLabel);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private JPanel createLauncherSelectionPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("실행기를 선택하세요:");
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        panel.add(title);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));

        JPanel gridPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));

        ButtonGroup group = new ButtonGroup();
        JToggleButton paperButton = utils.createLauncherButton("Paper", "/resources/papermc.png");
        paperButton.setActionCommand("Paper");
        paperButton.setSelected(true);

        group.add(paperButton);
        gridPanel.add(paperButton);
        panel.add(gridPanel);

        return panel;
    }

    private JPanel createEulaPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JTextArea eulaTextArea = new JTextArea();
        eulaTextArea.setEditable(false);
        eulaTextArea.setLineWrap(true);
        eulaTextArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(eulaTextArea);
        scrollPane.setPreferredSize(new Dimension(400, 150));
        scrollPane.setAlignmentX(Component.CENTER_ALIGNMENT);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                getClass().getResourceAsStream("/resources/eula.txt"), StandardCharsets.UTF_8))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            eulaTextArea.setText(content.toString());
            eulaTextArea.setCaretPosition(0);
        } catch (Exception e) {
            eulaTextArea.setText("EULA 파일을 불러올 수 없습니다.");
        }

        JCheckBox eulaCheckBox = new JCheckBox("EULA에 동의합니다.");
        eulaCheckBox.setAlignmentX(Component.CENTER_ALIGNMENT);
        eulaCheckBox.addActionListener(e -> {
            agreedEula = eulaCheckBox.isSelected();
            nextButton.setEnabled(agreedEula);
        });

        panel.add(Box.createVerticalGlue());
        panel.add(scrollPane);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(eulaCheckBox);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private void updateStepUI(int step, JLabel label, JButton previous, JButton next, JFrame frame) {
        frame.getContentPane().remove(2);

        switch (step) {
            case 1:
                label.setText("버전");
                frame.add(setversion, BorderLayout.CENTER);
                previous.setEnabled(false);
                next.setText("다음");
                next.setEnabled(true);
                break;
            case 2:
                label.setText("이름");
                frame.add(setNamePanel, BorderLayout.CENTER);
                previous.setEnabled(true);
                next.setText("다음");
                SwingUtilities.invokeLater(() -> {
                    String currentName = nameField.getText().trim();
                    Set<String> existingServers = utils.getExistingServerNames();
                    if (currentName.isEmpty()) {
                        warningLabel.setText("");
                        next.setEnabled(false);
                    } else if (existingServers.contains(currentName)) {
                        warningLabel.setText("이미 존재하는 서버 이름입니다.");
                        next.setEnabled(false);
                    } else {
                        warningLabel.setText("");
                        next.setEnabled(true);
                    }
                });
                break;
            case 3:
                label.setText("실행기");
                frame.add(launcherPanel, BorderLayout.CENTER);
                previous.setEnabled(true);
                next.setText("다음");
                next.setEnabled(true);
                break;
            case 4:
                label.setText("EULA");
                frame.add(eulaPanel, BorderLayout.CENTER);
                previous.setEnabled(true);
                next.setText("완료");
                next.setEnabled(agreedEula);
                break;
        }

        frame.revalidate();
        frame.repaint();
    }
}