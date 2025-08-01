import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.concurrent.ConcurrentHashMap;

public class Basket extends JFrame {
    // UI 컴포넌트
    public static JButton addButton;

    // 서버 관리 데이터
    private static JPanel serversPanel; // Servers 클래스 대신 직접 관리
    private static JPanel serversContainer; // 컨테이너 패널
    private static ConcurrentHashMap<String, Process> runningServers = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, JLabel> serverStatusLabels = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, JLabel> serverStatusLabels2 = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, JTextArea> serverConsoleAreas = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, JFrame> serverManageFrames = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, JPanel> serverBoxes = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        initializeServersPanel(); // Servers 객체 대신 패널 초기화
        loadSavedServers();
        createMainFrame();
        setupShutdownHook();
    }

    // Servers 클래스의 기능을 대체하는 메서드들
    private static void initializeServersPanel() {
        serversPanel = new JPanel();
        serversPanel.setLayout(new BoxLayout(serversPanel, BoxLayout.Y_AXIS));
        serversPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JScrollPane scrollPane = new JScrollPane(serversPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setWheelScrollingEnabled(true);
        scrollPane.getVerticalScrollBar().setUnitIncrement(8);

        serversContainer = new JPanel(new BorderLayout());
        serversContainer.add(scrollPane, BorderLayout.CENTER);
    }

    public static void addServerBox(JPanel box) {
        serversPanel.add(box);
        serversPanel.revalidate();
        serversPanel.repaint();
    }

    public static void removeServerBox(JPanel serverBox) {
        serversPanel.remove(serverBox);
        serversPanel.revalidate();
        serversPanel.repaint();
        updateServersSize();
    }

    private static void updateServersSize() {
        int componentCount = serversPanel.getComponentCount();
        serversPanel.setPreferredSize(new Dimension(580, componentCount * 85)); // 서버 박스 높이 고려
    }

    public static JPanel getServersPanel() {
        return serversContainer;
    }

    private static void createMainFrame() {
        JPanel navigation = createNavigationPanel();
        JScrollPane scrollPane = createServerScrollPane();

        JFrame mainFrame = new JFrame("Basket");
        mainFrame.setSize(600, 400);
        mainFrame.add(navigation, BorderLayout.NORTH);
        mainFrame.add(scrollPane, BorderLayout.CENTER);
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setResizable(false);
        mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        mainFrame.setVisible(true);
    }

    private static JPanel createNavigationPanel() {
        JPanel navigation = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        navigation.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.BLACK));
        navigation.setPreferredSize(new Dimension(600, 50));

        addButton = utils.createTextButton(100, 40, "서버 추가");
        addButton.addActionListener(e -> showServerCreationWizard());

        navigation.add(addButton);
        return navigation;
    }

    private static JScrollPane createServerScrollPane() {
        serversContainer.setPreferredSize(new Dimension(580, 0));

        JScrollPane scrollPane = new JScrollPane(serversContainer);
        scrollPane.setBorder(null);
        scrollPane.setWheelScrollingEnabled(false);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        return scrollPane;
    }

    private static void showServerCreationWizard() {
        addButton.setEnabled(false);

        // ServerWizard 사용
        ServerWizard wizard = new ServerWizard();
        wizard.showWizard((name, version, launcher, agreedEula) -> {
            utils.saveServerConfig(name, version, launcher, agreedEula.toString());
            JPanel serverBox = createServerBox(name, version);
            addServerBox(serverBox);
            serverBoxes.put(name, serverBox);
            addButton.setEnabled(true);
        });
    }

    private static void setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (Process process : runningServers.values()) {
                if (process.isAlive()) {
                    process.destroyForcibly();
                }
            }
        }));
    }

    // ===== 서버 박스 생성 및 관리 =====

    public static JPanel createServerBox(String name, String version) {
        JPanel box = new JPanel(new BorderLayout());
        box.setPreferredSize(new Dimension(550, 75));
        box.setMaximumSize(new Dimension(550, 75));
        box.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        JPanel leftPanel = createServerInfoPanel(name, version);
        JPanel rightPanel = createServerControlPanel(name, version);

        box.add(leftPanel, BorderLayout.WEST);
        box.add(rightPanel, BorderLayout.EAST);

        return box;
    }

    private static JPanel createServerInfoPanel(String name, String version) {
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 5));

        left.add(new JLabel(name));
        left.add(new JLabel(version));

        // 서버 상태 라벨들
        JLabel statusLabel1 = createStatusLabel("준비됨");
        JLabel statusLabel2 = createStatusLabel("");

        left.add(statusLabel1);
        left.add(statusLabel2);

        serverStatusLabels.put(name, statusLabel1);
        serverStatusLabels2.put(name, statusLabel2);

        return left;
    }

    private static JLabel createStatusLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.GRAY);
        label.setFont(new Font("Monospaced", Font.PLAIN, 9));
        label.setPreferredSize(new Dimension(200, 12));
        return label;
    }

    private static JPanel createServerControlPanel(String name, String version) {
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton launchButton = utils.createImageButton(30, 30, "/resources/play.png");
        JButton editButton = utils.createImageButton(30, 30, "/resources/edit.png");

        launchButton.addActionListener(e -> toggleServerState(name, launchButton));
        editButton.addActionListener(e -> openServerManageFrame(name, version));

        right.add(launchButton);
        right.add(editButton);

        return right;
    }

    // ===== 서버 실행/중지 관리 =====

    private static void toggleServerState(String serverName, JButton launchButton) {
        if (runningServers.containsKey(serverName)) {
            stopServer(serverName, launchButton);
        } else {
            startServer(serverName, launchButton);
        }
    }

    private static void startServer(String serverName, JButton launchButton) {
        File serverDir = utils.getServerDirectory(serverName);
        File paperJar = new File(serverDir, "paper.jar");

        if (!paperJar.exists()) {
            updateServerStatus(serverName, "paper.jar 파일이 없습니다", "");
            return;
        }

        try {
            // 로그 파일 초기화
            File logFile = utils.getLogFile(serverName);
            if (logFile.exists()) {
                new PrintWriter(logFile).close();
            }

            ProcessBuilder pb = new ProcessBuilder("java", "-jar", "paper.jar", "nogui");
            pb.directory(serverDir);
            pb.redirectErrorStream(true);

            Process process = pb.start();
            runningServers.put(serverName, process);

            SwingUtilities.invokeLater(() -> {
                launchButton.setIcon(utils.resizeIcon(utils.loadIcon("/resources/stop.png"), 24, 24));
                launchButton.setToolTipText("서버 중지");
                updateServerStatus(serverName, "서버 시작 중...", "");
            });

            new Thread(() -> monitorServerOutput(serverName, process, launchButton)).start();

        } catch (IOException e) {
            updateServerStatus(serverName, "실행 실패: " + e.getMessage(), "");
            e.printStackTrace();
        }
    }

    private static void stopServer(String serverName, JButton launchButton) {
        Process process = runningServers.get(serverName);
        if (process != null && process.isAlive()) {
            try {
                OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream());
                writer.write("stop\n");
                writer.flush();

                // 5초 후 강제 종료
                new Thread(() -> {
                    try {
                        Thread.sleep(5000);
                        if (process.isAlive()) {
                            process.destroyForcibly();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();

            } catch (IOException e) {
                process.destroyForcibly();
            }
        }
    }

    private static void monitorServerOutput(String serverName, Process process, JButton launchButton) {
        File logFile = utils.getLogFile(serverName);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
             BufferedWriter logWriter = new BufferedWriter(new FileWriter(logFile, true))) {

            String line;
            String[] lastTwoLines = {"", ""};

            while ((line = reader.readLine()) != null && process.isAlive()) {
                lastTwoLines[0] = lastTwoLines[1];
                lastTwoLines[1] = line;

                logWriter.write(line + "\n");
                logWriter.flush();

                final String finalLine = line;
                final String[] finalLastTwoLines = lastTwoLines.clone();

                SwingUtilities.invokeLater(() -> {
                    String cleanLine1 = utils.cleanLogLine(finalLastTwoLines[0]);
                    String cleanLine2 = utils.cleanLogLine(finalLastTwoLines[1]);
                    updateServerStatus(serverName, cleanLine1, cleanLine2);

                    JTextArea consoleArea = serverConsoleAreas.get(serverName);
                    if (consoleArea != null) {
                        consoleArea.append(finalLine + "\n");
                        consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
                    }
                });
            }

            SwingUtilities.invokeLater(() -> {
                runningServers.remove(serverName);
                launchButton.setIcon(utils.resizeIcon(utils.loadIcon("/resources/play.png"), 24, 24));
                launchButton.setToolTipText("서버 시작");
                updateServerStatus(serverName, "서버 중지됨", "");

                JTextArea consoleArea = serverConsoleAreas.get(serverName);
                if (consoleArea != null) {
                    consoleArea.append("서버가 중지되었습니다.\n");
                    consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
                }
            });

        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> updateServerStatus(serverName, "출력 읽기 실패", ""));
        }
    }

    // ===== 서버 관리 창 =====

    private static void openServerManageFrame(String serverName, String version) {
        if (serverManageFrames.containsKey(serverName)) {
            JFrame existingFrame = serverManageFrames.get(serverName);
            existingFrame.toFront();
            existingFrame.requestFocus();
            return;
        }

        JFrame manageFrame = createServerManageFrame(serverName, version);
        serverManageFrames.put(serverName, manageFrame);
        manageFrame.setVisible(true);
    }

    private static JFrame createServerManageFrame(String serverName, String version) {
        JFrame manageFrame = new JFrame("서버 관리 - " + serverName);
        manageFrame.setSize(700, 500);
        manageFrame.setLocationRelativeTo(null);
        manageFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel infoPanel = createServerInfoManagePanel(serverName, version, manageFrame);
        JScrollPane consoleScrollPane = createConsolePanel(serverName);
        JPanel commandPanel = createCommandPanel(serverName);

        manageFrame.setLayout(new BorderLayout());
        manageFrame.add(infoPanel, BorderLayout.NORTH);
        manageFrame.add(consoleScrollPane, BorderLayout.CENTER);
        manageFrame.add(commandPanel, BorderLayout.SOUTH);

        manageFrame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                serverManageFrames.remove(serverName);
                serverConsoleAreas.remove(serverName);
            }
        });

        return manageFrame;
    }

    private static JPanel createServerInfoManagePanel(String serverName, String version, JFrame manageFrame) {
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBorder(BorderFactory.createTitledBorder("서버 정보"));

        JPanel leftInfo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftInfo.add(new JLabel(serverName));
        leftInfo.add(new JLabel("     " + version));

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton openFolderButton = utils.createTextButton(90, 30, "폴더 열기");
        openFolderButton.addActionListener(e -> openServerFolder(serverName, manageFrame));

        JButton deleteButton = createDeleteButton(serverName, manageFrame);

        rightPanel.add(openFolderButton);
        rightPanel.add(deleteButton);

        infoPanel.add(leftInfo, BorderLayout.WEST);
        infoPanel.add(rightPanel, BorderLayout.EAST);

        return infoPanel;
    }

    private static JButton createDeleteButton(String serverName, JFrame manageFrame) {
        JButton deleteButton = utils.createTextButton(90, 30, "서버 삭제");
        deleteButton.setBackground(Color.RED);
        deleteButton.setForeground(Color.WHITE);
        deleteButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                    manageFrame,
                    "정말로 서버 '" + serverName + "'을(를) 삭제하시겠습니까?\n이 작업은 되돌릴 수 없습니다.",
                    "서버 삭제 확인",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (confirm == JOptionPane.YES_OPTION) {
                deleteServer(serverName);
                manageFrame.dispose();
            }
        });
        return deleteButton;
    }

    private static JScrollPane createConsolePanel(String serverName) {
        JTextArea consoleArea = new JTextArea();
        consoleArea.setEditable(false);
        consoleArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        consoleArea.setBackground(Color.BLACK);
        consoleArea.setForeground(Color.WHITE);

        JScrollPane consoleScrollPane = new JScrollPane(consoleArea);
        consoleScrollPane.setBorder(BorderFactory.createTitledBorder("콘솔 출력"));
        consoleScrollPane.setPreferredSize(new Dimension(680, 300));

        loadExistingLogs(serverName, consoleArea);
        serverConsoleAreas.put(serverName, consoleArea);

        return consoleScrollPane;
    }

    private static void loadExistingLogs(String serverName, JTextArea consoleArea) {
        File logFile = utils.getLogFile(serverName);
        if (logFile.exists()) {
            try (BufferedReader logReader = new BufferedReader(new FileReader(logFile))) {
                String logLine;
                while ((logLine = logReader.readLine()) != null) {
                    consoleArea.append(logLine + "\n");
                }
            } catch (IOException e) {
                consoleArea.append("로그 파일 읽기 실패: " + e.getMessage() + "\n");
            }
        }

        Process currentProcess = runningServers.get(serverName);
        if (currentProcess == null || !currentProcess.isAlive()) {
            consoleArea.append("서버가 중지되어 있습니다.\n");
        }
    }

    private static JPanel createCommandPanel(String serverName) {
        JPanel commandPanel = new JPanel(new BorderLayout());
        commandPanel.setBorder(BorderFactory.createTitledBorder("명령어"));

        JTextField commandField = new JTextField();
        commandField.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JButton sendButton = utils.createTextButton(60, 30, "전송");

        ActionListener sendCommand = e -> executeServerCommand(serverName, commandField);

        commandField.addActionListener(sendCommand);
        sendButton.addActionListener(sendCommand);

        commandPanel.add(commandField, BorderLayout.CENTER);
        commandPanel.add(sendButton, BorderLayout.EAST);

        return commandPanel;
    }

    private static void executeServerCommand(String serverName, JTextField commandField) {
        String command = commandField.getText().trim();
        if (command.isEmpty()) return;

        Process process = runningServers.get(serverName);
        JTextArea consoleArea = serverConsoleAreas.get(serverName);

        if (process != null && process.isAlive()) {
            try {
                OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream());
                writer.write(command + "\n");
                writer.flush();

                if (consoleArea != null) {
                    consoleArea.append("> " + command + "\n");
                    consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
                }
                commandField.setText("");
            } catch (IOException ex) {
                if (consoleArea != null) {
                    consoleArea.append("명령어 전송 실패: " + ex.getMessage() + "\n");
                }
            }
        } else {
            if (consoleArea != null) {
                consoleArea.append("서버가 실행 중이 아닙니다.\n");
            }
        }
    }

    // ===== 서버 삭제 관련 =====

    private static void deleteServer(String serverName) {
        // 실행 중인 서버 중지
        Process process = runningServers.get(serverName);
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
            runningServers.remove(serverName);
        }

        // 서버 폴더 삭제
        File serverDir = utils.getServerDirectory(serverName);
        if (serverDir.exists()) {
            utils.deleteDirectory(serverDir);
        }

        // UI에서 서버 제거
        removeServerFromUI(serverName);

        // 관련 데이터 정리
        cleanupServerData(serverName);

        JOptionPane.showMessageDialog(null,
                "서버 '" + serverName + "'이(가) 삭제되었습니다.",
                "서버 삭제 완료",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private static void removeServerFromUI(String serverName) {
        JPanel serverBox = serverBoxes.get(serverName);
        if (serverBox != null) {
            SwingUtilities.invokeLater(() -> {
                removeServerBox(serverBox);
                serverBoxes.remove(serverName);

                serversPanel.setPreferredSize(null);
                serversPanel.revalidate();
                serversPanel.repaint();
            });
        }
    }

    private static void cleanupServerData(String serverName) {
        serverStatusLabels.remove(serverName);
        serverStatusLabels2.remove(serverName);
        serverConsoleAreas.remove(serverName);

        JFrame manageFrame = serverManageFrames.get(serverName);
        if (manageFrame != null) {
            manageFrame.dispose();
            serverManageFrames.remove(serverName);
        }
    }

    private static void openServerFolder(String serverName, JFrame manageFrame) {
        File serverFolder = utils.getServerDirectory(serverName);
        if (serverFolder.exists()) {
            try {
                Desktop.getDesktop().open(serverFolder);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(manageFrame,
                        "폴더를 여는 중 오류가 발생했습니다: " + ex.getMessage(),
                        "오류",
                        JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(manageFrame,
                    "서버 폴더를 찾을 수 없습니다.",
                    "폴더 없음",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    // ===== 유틸리티 메서드들 =====

    private static void updateServerStatus(String serverName, String status1, String status2) {
        utils.updateServerStatus(serverName, status1, status2, serverStatusLabels, serverStatusLabels2);
    }

    private static void loadSavedServers() {
        File dir = new File("servers");
        if (!dir.exists()) return;

        File[] serverDirs = dir.listFiles(File::isDirectory);
        if (serverDirs == null) return;

        for (File serverDir : serverDirs) {
            File settingsFile = new File(serverDir, "settings.txt");
            if (!settingsFile.exists()) continue;

            try (BufferedReader reader = new BufferedReader(new FileReader(settingsFile))) {
                String name = null, version = null;
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("name=")) name = line.substring(5);
                    if (line.startsWith("version=")) version = line.substring(8);
                }
                if (name != null && version != null) {
                    JPanel serverBox = createServerBox(name, version);
                    addServerBox(serverBox);
                    serverBoxes.put(name, serverBox);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public interface ServerCreationCallback {
        void onServerCreated(String name, String version, String launcher, Boolean eula);
    }
}