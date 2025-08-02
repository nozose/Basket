import Settings.Settings;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentHashMap;
import org.json.*;

public class utils {

    // ===== UI 컴포넌트 생성 블록들 =====

    public static JButton createTextButton(Integer width, Integer height, String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(width, height));
        return button;
    }

    public static JButton createImageButton(Integer width, Integer height, String path) {
        JButton button = new JButton(resizeIcon(loadIcon(path), width - 10, height - 10));
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(width, height));
        return button;
    }

    public static JPanel createNavigationPanel(int width, int height, JButton... buttons) {
        JPanel navigation = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        navigation.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.BLACK));
        navigation.setPreferredSize(new Dimension(width, height));

        for (JButton button : buttons) {
            navigation.add(button);
        }

        return navigation;
    }

    public static JScrollPane createScrollPane(JComponent component, boolean horizontalScroll, boolean verticalScroll) {
        JScrollPane scrollPane = new JScrollPane(component);
        scrollPane.setBorder(null);
        scrollPane.setWheelScrollingEnabled(true);
        scrollPane.setHorizontalScrollBarPolicy(horizontalScroll ?
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED : JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(verticalScroll ?
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED : JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(8);
        return scrollPane;
    }

    public static JFrame createMainFrame(String title, int width, int height, JComponent... components) {
        JFrame frame = new JFrame(title);
        frame.setSize(width, height);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        if (components.length >= 1) frame.add(components[0], BorderLayout.NORTH);
        if (components.length >= 2) frame.add(components[1], BorderLayout.CENTER);
        if (components.length >= 3) frame.add(components[2], BorderLayout.SOUTH);

        return frame;
    }



    // ===== 서버 박스 생성 블록들 =====

    public static JPanel createServerBox(String name, String version,
                                         ConcurrentHashMap<String, JLabel> statusLabels1,
                                         ConcurrentHashMap<String, JLabel> statusLabels2,
                                         ActionListener launchAction,
                                         ActionListener editAction) {
        JPanel box = new JPanel(new BorderLayout());
        box.setPreferredSize(new Dimension(550, 75));
        box.setMaximumSize(new Dimension(550, 75));
        box.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        JPanel leftPanel = createServerInfoPanel(name, version, statusLabels1, statusLabels2);
        JPanel rightPanel = createServerControlPanel(launchAction, editAction);

        box.add(leftPanel, BorderLayout.WEST);
        box.add(rightPanel, BorderLayout.EAST);

        return box;
    }

    public static JPanel createServerInfoPanel(String name, String version,
                                               ConcurrentHashMap<String, JLabel> statusLabels1,
                                               ConcurrentHashMap<String, JLabel> statusLabels2) {
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 8));

        // Create a panel for name and version on the same line with zero padding
        JPanel nameVersionPanel = new JPanel();
        nameVersionPanel.setLayout(new BoxLayout(nameVersionPanel, BoxLayout.X_AXIS));
        nameVersionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel nameLabel = new JLabel(name);
        JLabel separatorLabel = new JLabel(" | ");
        separatorLabel.setForeground(Color.GRAY);
        JLabel versionLabel = new JLabel(version);
        versionLabel.setForeground(Color.GRAY);
        
        nameVersionPanel.add(nameLabel);
        nameVersionPanel.add(separatorLabel);
        nameVersionPanel.add(versionLabel);
        nameVersionPanel.add(Box.createHorizontalGlue()); // Push everything to the left
        
        left.add(nameVersionPanel);

        // Add top padding before status labels
        left.add(Box.createVerticalStrut(7));
        // Status labels with proper left alignment and line breaks
        JLabel statusLabel1 = createStatusLabel("준비됨");
        JLabel statusLabel2 = createStatusLabel("");
        
        // Ensure labels are left-aligned
        statusLabel1.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusLabel2.setAlignmentX(Component.LEFT_ALIGNMENT);

        left.add(statusLabel1);
        left.add(statusLabel2);

        statusLabels1.put(name, statusLabel1);
        statusLabels2.put(name, statusLabel2);

        return left;
    }

    public static JLabel createStatusLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.GRAY);
        label.setFont(new Font("Monospaced", Font.PLAIN, 9));
        label.setPreferredSize(new Dimension(200, 12));
        return label;
    }

    public static JPanel createServerControlPanel(ActionListener launchAction, ActionListener editAction) {
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 8));
        right.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 12));

        JButton launchButton = createImageButton(30, 30, "/resources/play.png");
        JButton editButton = createImageButton(30, 30, "/resources/edit.png");

        launchButton.addActionListener(launchAction);
        editButton.addActionListener(editAction);

        right.add(launchButton);
        right.add(editButton);

        return right;
    }



    // ===== 서버 프로세스 관리 블록들 =====

    public static Process startServerProcess(String serverName) throws IOException {
        File serverDir = getServerDirectory(serverName);
        File paperJar = new File(serverDir, "paper.jar");

        if (!paperJar.exists()) {
            throw new FileNotFoundException("paper.jar 파일이 없습니다");
        }

        // 로그 파일 초기화
        File logFile = getLogFile(serverName);
        if (logFile.exists()) {
            new PrintWriter(logFile).close();
        }

        ProcessBuilder pb = new ProcessBuilder("java", "-jar", "paper.jar", "nogui");
        pb.directory(serverDir);
        pb.redirectErrorStream(true);

        return pb.start();
    }

    public static void stopServerProcess(Process process) {
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

    public static void monitorServerOutput(String serverName, Process process,
                                           ConcurrentHashMap<String, JLabel> statusLabels1,
                                           ConcurrentHashMap<String, JLabel> statusLabels2,
                                           ConcurrentHashMap<String, JTextArea> consoleAreas,
                                           Runnable onProcessEnd) {
        File logFile = getLogFile(serverName);

        new Thread(() -> {
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
                        String cleanLine1 = cleanLogLine(finalLastTwoLines[0]);
                        String cleanLine2 = cleanLogLine(finalLastTwoLines[1]);
                        updateServerStatus(serverName, cleanLine1, cleanLine2, statusLabels1, statusLabels2);

                        JTextArea consoleArea = consoleAreas.get(serverName);
                        if (consoleArea != null) {
                            consoleArea.append(finalLine + "\n");
                            consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
                        }
                    });
                }

                SwingUtilities.invokeLater(onProcessEnd);

            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> updateServerStatus(serverName, "출력 읽기 실패", "", statusLabels1, statusLabels2));
            }
        }).start();
    }

    public static void sendServerCommand(String serverName, String command,
                                         ConcurrentHashMap<String, Process> runningServers,
                                         ConcurrentHashMap<String, JTextArea> consoleAreas) {
        if (command.trim().isEmpty()) return;

        Process process = runningServers.get(serverName);
        JTextArea consoleArea = consoleAreas.get(serverName);

        if (process != null && process.isAlive()) {
            try {
                OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream());
                writer.write(command + "\n");
                writer.flush();

                if (consoleArea != null) {
                    consoleArea.append("> " + command + "\n");
                    consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
                }
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



    // ===== 서버 관리 창 생성 블록들 =====

    public static JFrame createServerManageFrame(String serverName, String version) {
        JFrame manageFrame = new JFrame("서버 관리 - " + serverName);
        manageFrame.setSize(700, 500);
        manageFrame.setLocationRelativeTo(null);
        manageFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        return manageFrame;
    }

    public static JPanel createServerInfoManagePanel(String serverName, String version,
                                                     ActionListener openFolderAction,
                                                     ActionListener deleteAction) {
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBorder(BorderFactory.createTitledBorder("서버 정보"));

        JPanel leftInfo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftInfo.add(new JLabel(serverName));
        leftInfo.add(new JLabel("     " + version));

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton openFolderButton = createTextButton(90, 30, "폴더 열기");
        openFolderButton.addActionListener(openFolderAction);

        JButton deleteButton = createTextButton(90, 30, "서버 삭제");
        deleteButton.setBackground(Color.RED);
        deleteButton.setForeground(Color.WHITE);
        deleteButton.addActionListener(deleteAction);

        rightPanel.add(openFolderButton);
        rightPanel.add(deleteButton);

        infoPanel.add(leftInfo, BorderLayout.WEST);
        infoPanel.add(rightPanel, BorderLayout.EAST);

        return infoPanel;
    }

    public static JScrollPane createConsolePanel(String serverName,
                                                 ConcurrentHashMap<String, JTextArea> consoleAreas,
                                                 ConcurrentHashMap<String, Process> runningServers) {
        JTextArea consoleArea = new JTextArea();
        consoleArea.setEditable(false);
        consoleArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        consoleArea.setBackground(Color.BLACK);
        consoleArea.setForeground(Color.WHITE);

        JScrollPane consoleScrollPane = new JScrollPane(consoleArea);
        consoleScrollPane.setBorder(BorderFactory.createTitledBorder("콘솔 출력"));
        consoleScrollPane.setPreferredSize(new Dimension(680, 300));

        loadExistingLogs(serverName, consoleArea, runningServers);
        consoleAreas.put(serverName, consoleArea);

        return consoleScrollPane;
    }

    public static JPanel createCommandPanel(String serverName,
                                            ConcurrentHashMap<String, Process> runningServers,
                                            ConcurrentHashMap<String, JTextArea> consoleAreas) {
        JPanel commandPanel = new JPanel(new BorderLayout());
        commandPanel.setBorder(BorderFactory.createTitledBorder("명령어"));

        JTextField commandField = new JTextField();
        commandField.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JButton sendButton = createTextButton(60, 30, "전송");

        ActionListener sendCommand = e -> {
            String command = commandField.getText().trim();
            sendServerCommand(serverName, command, runningServers, consoleAreas);
            commandField.setText("");
        };

        commandField.addActionListener(sendCommand);
        sendButton.addActionListener(sendCommand);

        commandPanel.add(commandField, BorderLayout.CENTER);
        commandPanel.add(sendButton, BorderLayout.EAST);

        return commandPanel;
    }



    // ===== 서버 삭제 관련 블록들 =====

    public static boolean confirmServerDeletion(String serverName, Component parent) {
        int confirm = JOptionPane.showConfirmDialog(
                parent,
                "정말로 서버 '" + serverName + "'을(를) 삭제하시겠습니까?\n이 작업은 되돌릴 수 없습니다.",
                "서버 삭제 확인",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        return confirm == JOptionPane.YES_OPTION;
    }

    public static void deleteServerCompletely(String serverName,
                                              ConcurrentHashMap<String, Process> runningServers,
                                              ConcurrentHashMap<String, JLabel> statusLabels1,
                                              ConcurrentHashMap<String, JLabel> statusLabels2,
                                              ConcurrentHashMap<String, JTextArea> consoleAreas,
                                              ConcurrentHashMap<String, JFrame> manageFrames,
                                              ConcurrentHashMap<String, JPanel> serverBoxes,
                                              JPanel serversPanel) {
        // 실행 중인 서버 중지
        Process process = runningServers.get(serverName);
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
            runningServers.remove(serverName);
        }

        // 서버 폴더 삭제
        File serverDir = getServerDirectory(serverName);
        if (serverDir.exists()) {
            deleteDirectory(serverDir);
        }

        // UI에서 서버 제거 (서버 박스와 그 다음 간격 컴포넌트 모두 제거)
        JPanel serverBox = serverBoxes.get(serverName);
        if (serverBox != null) {
            SwingUtilities.invokeLater(() -> {
                int serverBoxIndex = -1;
                Component[] components = serversPanel.getComponents();
                for (int i = 0; i < components.length; i++) {
                    if (components[i] == serverBox) {
                        serverBoxIndex = i;
                        break;
                    }
                }
                
                if (serverBoxIndex != -1) {
                    // 서버 박스 제거
                    serversPanel.remove(serverBoxIndex);
                    // 다음 컴포넌트가 간격인지 확인하고 제거
                    if (serverBoxIndex < serversPanel.getComponentCount() && 
                        serversPanel.getComponent(serverBoxIndex) instanceof Box.Filler) {
                        serversPanel.remove(serverBoxIndex);
                    }
                }
                
                serversPanel.revalidate();
                serversPanel.repaint();
                serverBoxes.remove(serverName);
            });
        }

        // 관련 데이터 정리
        statusLabels1.remove(serverName);
        statusLabels2.remove(serverName);
        consoleAreas.remove(serverName);

        JFrame manageFrame = manageFrames.get(serverName);
        if (manageFrame != null) {
            manageFrame.dispose();
            manageFrames.remove(serverName);
        }

        JOptionPane.showMessageDialog(null,
                "서버 '" + serverName + "'이(가) 삭제되었습니다.",
                "서버 삭제 완료",
                JOptionPane.INFORMATION_MESSAGE);
    }

    public static void openServerFolder(String serverName, Component parent) {
        File serverFolder = getServerDirectory(serverName);
        if (serverFolder.exists()) {
            try {
                String os = System.getProperty("os.name").toLowerCase();
                ProcessBuilder pb;
                
                if (os.contains("mac") || os.contains("darwin")) {
                    // macOS
                    pb = new ProcessBuilder("open", serverFolder.getAbsolutePath());
                } else if (os.contains("win")) {
                    // Windows
                    pb = new ProcessBuilder("explorer.exe", serverFolder.getAbsolutePath());
                } else {
                    // Linux/Unix
                    pb = new ProcessBuilder("xdg-open", serverFolder.getAbsolutePath());
                }
                
                pb.start();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(parent,
                        "폴더를 여는 중 오류가 발생했습니다: " + ex.getMessage(),
                        "오류",
                        JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(parent,
                    "서버 폴더를 찾을 수 없습니다.",
                    "폴더 없음",
                    JOptionPane.WARNING_MESSAGE);
        }
    }



    // ===== 서버 로딩 관련 블록들 =====

    public static void loadAndCreateSavedServers(JPanel serversPanel,
                                                 ConcurrentHashMap<String, JPanel> serverBoxes,
                                                 ConcurrentHashMap<String, JLabel> statusLabels1,
                                                 ConcurrentHashMap<String, JLabel> statusLabels2,
                                                 ActionListener launchActionProvider,
                                                 ActionListener editActionProvider) {
        Settings settings = new Settings();
        String serversPath = settings.getPathOrDefault("servers");
        File dir = new File(serversPath);
        if (!dir.exists()) {
            // 디렉토리가 없으면 생성 시도
            if (!dir.mkdirs()) {
                System.err.println("서버 디렉토리 생성 실패: " + serversPath);
                return;
            }
        }

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
                    JPanel serverBox = createServerBox(name, version, statusLabels1, statusLabels2,
                            launchActionProvider, editActionProvider);
                    // 글루 전에 삽입
                    int insertIndex = serversPanel.getComponentCount() - 1;
                    serversPanel.add(serverBox, insertIndex);
                    serversPanel.add(Box.createRigidArea(new Dimension(0, 8)), insertIndex + 1);
                    serverBoxes.put(name, serverBox);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        serversPanel.revalidate();
        serversPanel.repaint();
    }

    public static void setupShutdownHook(ConcurrentHashMap<String, Process> runningServers) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (Process process : runningServers.values()) {
                if (process.isAlive()) {
                    process.destroyForcibly();
                }
            }
        }));
    }



    // ===== 이미지 관련 유틸리티 =====

    public static ImageIcon loadIcon(String path) {
        try {
            return new ImageIcon(Objects.requireNonNull(utils.class.getResource(path)));
        } catch (Exception e) {
            System.err.println("이미지 로드 실패: " + path);
            return new ImageIcon();
        }
    }

    public static ImageIcon resizeIcon(ImageIcon icon, int width, int height) {
        int size = Math.min(width, height);
        Image img = icon.getImage();
        Image resized = img.getScaledInstance(size, size, Image.SCALE_SMOOTH);
        return new ImageIcon(resized);
    }



    // ===== 파일/디렉토리 관련 유틸리티 =====

    public static String sanitizeFileName(String fileName) {
        if (fileName == null) return "server";

        String sanitized = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        sanitized = sanitized.replaceAll("[\\x00-\\x1F]", "_");
        sanitized = sanitized.replaceAll("[.\\s]+$", "");

        if (sanitized.isEmpty() || isReservedName(sanitized)) {
            sanitized = "server_" + System.currentTimeMillis();
        }

        if (sanitized.length() > 100) {
            sanitized = sanitized.substring(0, 100);
        }

        return sanitized;
    }

    public static boolean isReservedName(String name) {
        String[] reserved = {"CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3", "COM4",
                "COM5", "COM6", "COM7", "COM8", "COM9", "LPT1", "LPT2",
                "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"};

        for (String res : reserved) {
            if (res.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    public static File getServerDirectory(String serverName) {
        Settings settings = new Settings();
        String serversPath = settings.getPathOrDefault("servers");
        String sanitizedName = sanitizeFileName(serverName);
        return new File(serversPath, sanitizedName);
    }

    public static File getLogFile(String serverName) {
        return new File(getServerDirectory(serverName), "console.log");
    }

    public static void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }



    // ===== 로그 처리 관련 유틸리티 =====

    public static String cleanLogLine(String line) {
        if (line == null || line.isEmpty()) return "";

        if (line.contains("]: ")) {
            int index = line.indexOf("]: ");
            if (index != -1 && index + 3 < line.length()) {
                String cleanedLine = line.substring(index + 3);
                if (cleanedLine.length() > 50) {
                    cleanedLine = cleanedLine.substring(0, 47) + "...";
                }
                return cleanedLine;
            }
        }

        if (line.length() > 50) {
            line = line.substring(0, 47) + "...";
        }

        return line;
    }

    private static void loadExistingLogs(String serverName, JTextArea consoleArea, ConcurrentHashMap<String, Process> runningServers) {
        File logFile = getLogFile(serverName);
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
        
        // Only show message when server is stopped
        Process serverProcess = runningServers.get(serverName);
        if (serverProcess == null || !serverProcess.isAlive()) {
            consoleArea.append("서버가 중지되어 있습니다.\n");
        }
        // Don't show anything when server is running
    }



    // ===== EULA 파일 생성 =====

    public static void createEulaFile(File serverDir, String eulaAccepted) {
        File eulaFile = new File(serverDir, "eula.txt");
        try (PrintWriter writer = new PrintWriter(eulaFile)) {
            writer.println("#By changing the setting below to TRUE you are indicating your agreement to our EULA (https://account.mojang.com/documents/minecraft_eula).");
            writer.println("#" + new java.util.Date());
            writer.println("eula=" + eulaAccepted.toLowerCase());
            System.out.println("eula.txt 파일 생성 완료: " + eulaFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("eula.txt 파일 생성 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }



    // ===== 서버 관리 관련 유틸리티 =====

    public static void updateServerStatus(String serverName, String status1, String status2,
                                          ConcurrentHashMap<String, JLabel> serverStatusLabels,
                                          ConcurrentHashMap<String, JLabel> serverStatusLabels2) {
        JLabel statusLabel1 = serverStatusLabels.get(serverName);
        JLabel statusLabel2 = serverStatusLabels2.get(serverName);

        if (statusLabel1 != null) {
            String shortStatus1 = status1 != null && status1.length() > 30 ? status1.substring(0, 27) + "..." : status1;
            statusLabel1.setText(shortStatus1);
            statusLabel1.setToolTipText(status1);
        }

        if (statusLabel2 != null) {
            String shortStatus2 = status2 != null && status2.length() > 30 ? status2.substring(0, 27) + "..." : status2;
            statusLabel2.setText(shortStatus2);
            statusLabel2.setToolTipText(status2);
        }
    }

    public static Set<String> getExistingServerNames() {
        Set<String> existingNames = new HashSet<>();
        File serversDir = new File("servers");

        if (!serversDir.exists()) {
            return existingNames;
        }

        File[] serverDirs = serversDir.listFiles(File::isDirectory);
        if (serverDirs == null) {
            return existingNames;
        }

        for (File serverDir : serverDirs) {
            File settingsFile = new File(serverDir, "settings.txt");
            if (settingsFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(settingsFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("name=")) {
                            String name = line.substring(5);
                            existingNames.add(name);
                            break;
                        }
                    }
                } catch (Exception e) {
                    existingNames.add(serverDir.getName());
                }
            }
        }

        return existingNames;
    }

    public static void saveServerConfig(String name, String version, String launcher, String eulaAccepted) {
        try {
            File serverDir = getServerDirectory(name);
            if (!serverDir.exists()) {
                if (!serverDir.mkdirs()) {
                    throw new IOException("서버 디렉토리 생성 실패: " + serverDir.getAbsolutePath());
                }
            }

            File settingsFile = new File(serverDir, "settings.txt");
            try (PrintWriter writer = new PrintWriter(new FileWriter(settingsFile))) {
                writer.println("name=" + name);
                writer.println("version=" + version);
                writer.println("launcher=" + launcher);
                writer.println("eula=" + eulaAccepted);
                writer.println("created=" + new java.util.Date());

                System.out.println("서버 설정 저장 완료: " + settingsFile.getAbsolutePath());
            }

            createEulaFile(serverDir, eulaAccepted);

            new Thread(() -> {
                try {
                    File paperJar = new File(serverDir, "paper.jar");
                    downloadLatestPaperJar(version, paperJar);
                    System.out.println("서버 JAR 파일 다운로드 완료: " + name);
                } catch (Exception e) {
                    System.err.println("서버 JAR 파일 다운로드 실패 (" + name + "): " + e.getMessage());
                    e.printStackTrace();
                }
            }).start();

        } catch (IOException e) {
            System.err.println("서버 설정 저장 실패: " + e.getMessage());
            e.printStackTrace();

            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(null,
                        "서버 설정 저장 중 오류가 발생했습니다:\n" + e.getMessage(),
                        "설정 저장 오류",
                        JOptionPane.ERROR_MESSAGE);
            });
        }
    }



    // ===== 네트워크/API 관련 유틸리티 =====

    public static List<String> getMinecraftVersions() throws Exception {
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

        versionList.sort((v1, v2) -> compareVersions(v2, v1));
        return versionList;
    }

    public static void downloadLatestPaperJar(String version, File destination) throws Exception {
        String buildsApiUrl = "https://api.papermc.io/v2/projects/paper/versions/" + version + "/builds";
        HttpURLConnection conn = (HttpURLConnection) new URL(buildsApiUrl).openConnection();
        conn.setRequestMethod("GET");

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        JSONObject json = new JSONObject(response.toString());
        JSONArray builds = json.getJSONArray("builds");
        if (builds.length() == 0) throw new IOException("해당 버전에 대한 빌드 없음");

        JSONObject latestBuildObj = builds.getJSONObject(builds.length() - 1);
        int latestBuild = latestBuildObj.getInt("build");

        String jarName = "paper-" + version + "-" + latestBuild + ".jar";
        String downloadUrl = String.format(
                "https://api.papermc.io/v2/projects/paper/versions/%s/builds/%d/downloads/%s",
                version, latestBuild, jarName
        );

        try (BufferedInputStream in = new BufferedInputStream(new URL(downloadUrl).openStream());
             FileOutputStream out = new FileOutputStream(destination)) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer, 0, buffer.length)) != -1) {
                out.write(buffer, 0, bytesRead);
            }

            System.out.println("paper.jar 다운로드 완료: " + destination.getAbsolutePath());
        }
    }



    // ===== 버전 비교 관련 유틸리티 =====

    public static int compareVersions(String v1, String v2) {
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

    public static int parseVersionPart(String part) {
        try {
            return Integer.parseInt(part.replaceAll("\\D.*", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }


}