import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.ArrayList;
import org.json.*;

public class Servers {
    private JPanel servers;
    private JPanel container;
    private JPanel setversion;
    private JPanel setNamePanel;
    private JPanel launcherPanel;
    private JPanel eulaPanel;
    private JTextField nameField;
    private JButton nextButton;

    // 저장할 임시 데이터
    private String selectedVersion = null;
    private String serverName = "";
    private String selectedLauncher = "paper";
    private boolean agreedEula = false;

    private Basket.ServerCreationCallback callback;

    public void addServerBox(JPanel box) {
        servers.add(box);
        servers.revalidate();
        servers.repaint();
    }

    public void removeServerBox(JPanel serverBox) {
        servers.remove(serverBox);
        servers.revalidate();
        servers.repaint();

        // 서버 패널의 크기 재조정
        updateServersSize();
    }

    private void updateServersSize() {
        int componentCount = servers.getComponentCount();
        int height = componentCount * 110; // 각 서버 박스 높이 + 여백
        servers.setPreferredSize(new Dimension(580, height));
    }

    public Servers() {
        servers = new JPanel();
        servers.setLayout(new BoxLayout(servers, BoxLayout.Y_AXIS));
        servers.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JScrollPane scrollPane = new JScrollPane(servers);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setWheelScrollingEnabled(true);
        scrollPane.getVerticalScrollBar().setUnitIncrement(8);

        container = new JPanel(new BorderLayout());
        container.add(scrollPane, BorderLayout.CENTER);
    }

    public JPanel getServers() {
        return container;
    }

    public void addServerComponent(Basket.ServerCreationCallback callback) {
        final int[] current = {1};

        JFrame addServerFrame = new JFrame();
        addServerFrame.setSize(540, 360);
        addServerFrame.setLocationRelativeTo(null);
        addServerFrame.setResizable(false);
        addServerFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // 창이 닫힐 때 addButton을 다시 활성화
        addServerFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                // addButton을 다시 활성화
                if (Basket.addButton != null) {
                    Basket.addButton.setEnabled(true);
                }
            }
        });

        JLabel currentDoing = new JLabel("버전");
        currentDoing.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        labelPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        labelPanel.add(currentDoing);
        addServerFrame.add(labelPanel, BorderLayout.NORTH);

        JPanel navigation = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        JButton previous = new JButton("이전");
        nextButton = new JButton("다음");
        previous.setEnabled(false);

        navigation.add(previous);
        navigation.add(nextButton);
        addServerFrame.add(navigation, BorderLayout.SOUTH);

        setversion = createVersionPanel();
        setNamePanel = createNamePanel();
        launcherPanel = createLauncherSelectionPanel();
        eulaPanel = createEulaPanel();

        addServerFrame.add(setversion, BorderLayout.CENTER);

        nextButton.addActionListener(e -> {
            if (nextButton.getText().equals("완료")) {
                createSummaryPanel();
                callback.onServerCreated(serverName, selectedVersion, selectedLauncher, agreedEula);
                addServerFrame.dispose();
                return;
            }
            current[0]++;
            updateStepUI(current[0], currentDoing, previous, nextButton, addServerFrame);
        });

        previous.addActionListener(e -> {
            if (current[0] > 1) {
                current[0]--;
                updateStepUI(current[0], currentDoing, previous, nextButton, addServerFrame);
            }
        });

        addServerFrame.setVisible(true);
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

        // PaperMC 버튼 생성 (정사각형, 이미지 포함, 라디오 버튼 역할)
        JToggleButton paperButton = createLauncherButton("Paper", "/resources/papermc.png");
        paperButton.setActionCommand("paper");
        paperButton.setSelected(true);

        group.add(paperButton);
        gridPanel.add(paperButton);

        panel.add(gridPanel);

        return panel;
    }

    private JToggleButton createLauncherButton(String name, String imagePath) {
        JToggleButton button = new JToggleButton();
        button.setPreferredSize(new Dimension(120, 120));
        button.setLayout(new BorderLayout());
        button.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        button.setFocusPainted(false);

        JLabel imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);

        try {
            ImageIcon icon = new ImageIcon(getClass().getResource(imagePath));
            Image scaled = icon.getImage().getScaledInstance(48, 48, Image.SCALE_SMOOTH);
            imageLabel.setIcon(new ImageIcon(scaled));
        } catch (Exception e) {
            imageLabel.setText("[이미지 없음]");
        }

        JLabel nameLabel = new JLabel(name, SwingConstants.CENTER);
        nameLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));

        JPanel innerPanel = new JPanel();
        innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.Y_AXIS));
        innerPanel.setOpaque(false);
        imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        innerPanel.add(Box.createVerticalGlue());
        innerPanel.add(imageLabel);
        innerPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        innerPanel.add(nameLabel);
        innerPanel.add(Box.createVerticalGlue());

        button.add(innerPanel, BorderLayout.CENTER);

        button.addChangeListener(e -> {
            if (button.isSelected()) {
                button.setBorder(BorderFactory.createLineBorder(new Color(100, 149, 237), 2));
            } else {
                button.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
            }
        });

        return button;
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
                return getMinecraftVersions();
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

        nameField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { validateName(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { validateName(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { validateName(); }

            public void validateName() {
                serverName = nameField.getText().trim();
                nextButton.setEnabled(!serverName.isEmpty());
            }
        });

        panel.add(Box.createVerticalGlue());
        panel.add(label);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(nameField);
        panel.add(Box.createVerticalGlue());

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

        // 파일에서 EULA 내용 읽기
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

    private JPanel createSummaryPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("서버 설정 요약");
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));

        JLabel versionLabel = new JLabel("버전: " + selectedVersion);
        versionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel nameLabel = new JLabel("서버 이름: " + serverName);
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel launcherLabel = new JLabel("실행기: " + selectedLauncher);
        launcherLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel eulaLabel = new JLabel("EULA 동의: " + (agreedEula ? "예" : "아니오"));
        eulaLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(Box.createVerticalGlue());
        panel.add(titleLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(versionLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(nameLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(launcherLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(eulaLabel);
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
                next.setEnabled(!nameField.getText().trim().isEmpty());
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
                next.setText("다음");
                next.setEnabled(agreedEula);
                break;
            case 5:
                label.setText("요약");
                JPanel summaryPanel = createSummaryPanel();
                frame.add(summaryPanel, BorderLayout.CENTER);
                previous.setEnabled(true);
                next.setText("완료");
                next.setEnabled(true);
                break;
        }

        frame.revalidate();
        frame.repaint();
    }

    private List<String> getMinecraftVersions() throws Exception {
        URL url = new URL("https://api.papermc.io/v2/projects/paper");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        InputStreamReader reader = new InputStreamReader(conn.getInputStream());
        StringBuilder sb = new StringBuilder();
        int ch;
        while ((ch = reader.read()) != -1) {
            sb.append((char) ch);
        }

        JSONObject json = new JSONObject(sb.toString());
        JSONArray versions = json.getJSONArray("versions");

        List<String> versionList = new ArrayList<>();
        for (int i = 0; i < versions.length(); i++) {
            versionList.add(versions.getString(i));
        }

        // 버전 숫자 기준 내림차순 정렬
        versionList.sort((v1, v2) -> compareVersions(v2, v1));

        return versionList;
    }

    // 버전 문자열을 숫자 단위로 비교 (예: 1.20.4 vs 1.9.4)
    private int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");

        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int num1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
            int num2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;
            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
        }
        return 0;
    }

    // 숫자가 아닌 접미사가 있는 버전에도 대응 (예: "1.13-pre7")
    private int parseVersionPart(String part) {
        try {
            return Integer.parseInt(part.replaceAll("\\D.*", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}