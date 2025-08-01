import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class Basket extends JFrame {
    public static JButton addButton;

    private static Servers servers; // static으로 변경하여 전역 접근 가능
    private static ConcurrentHashMap<String, Process> runningServers = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, JLabel> serverStatusLabels = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, JLabel> serverStatusLabels2 = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, JTextArea> serverConsoleAreas = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, JFrame> serverManageFrames = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, JPanel> serverBoxes = new ConcurrentHashMap<>(); // 서버 박스 추적용

    public static void main(String[] args) {

        servers = new Servers(); // static 변수로 할당

        // 서버 설정 파일 로드
        loadSavedServers(servers);

        JPanel navigation = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        navigation.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.BLACK));
        navigation.setPreferredSize(new Dimension(600, 50));

        addButton = new JButton("서버 추가");
        addButton.setFocusPainted(false);        // 포커스 표시 제거
        addButton.setPreferredSize(new Dimension(100, 40));
        navigation.add(addButton);

        addButton.addActionListener(e -> {
            addButton.setEnabled(false);
            servers.addServerComponent((name, version, launcher, agreedEula) -> {
                saveServerConfig(name, version, launcher, agreedEula.toString());
                JPanel serverBox = createServerBox(name, version);
                servers.addServerBox(serverBox);
                serverBoxes.put(name, serverBox); // 서버 박스 추적
                addButton.setEnabled(true);
            });
        });

        JPanel serversPanel = servers.getServers();
        serversPanel.setPreferredSize(new Dimension(580, 0));

        JScrollPane scrollPane = new JScrollPane(serversPanel);
        scrollPane.setBorder(null);
        scrollPane.setWheelScrollingEnabled(false);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        JFrame mainFrame = new JFrame();
        mainFrame.setTitle("Basket");
        mainFrame.setSize(600, 400);
        mainFrame.add(navigation, BorderLayout.NORTH);
        mainFrame.add(scrollPane, BorderLayout.CENTER);
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setResizable(false);
        mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        // 프로그램 종료 시 모든 서버 프로세스 정리
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (Process process : runningServers.values()) {
                if (process.isAlive()) {
                    process.destroyForcibly();
                }
            }
        }));

        mainFrame.setVisible(true);
    }

    public static JPanel createServerBox(String name, String version) {
        JPanel box = new JPanel(new BorderLayout());
        box.setPreferredSize(new Dimension(550, 75));
        box.setMaximumSize(new Dimension(550, 75));
        box.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 5));
        left.add(new JLabel(name));
        left.add(new JLabel(version));

        // 서버 상태 라벨 2개 추가 (2줄 표시)
        JLabel statusLabel1 = new JLabel("준비됨");
        statusLabel1.setForeground(Color.GRAY);
        statusLabel1.setFont(new Font("Monospaced", Font.PLAIN, 9));
        statusLabel1.setPreferredSize(new Dimension(200, 12)); // 폭/높이 고정
        left.add(statusLabel1);

        JLabel statusLabel2 = new JLabel("");
        statusLabel2.setForeground(Color.GRAY);
        statusLabel2.setFont(new Font("Monospaced", Font.PLAIN, 9));
        statusLabel2.setPreferredSize(new Dimension(200, 12)); // 폭/높이 고정
        left.add(statusLabel2);

        serverStatusLabels.put(name, statusLabel1);
        serverStatusLabels2.put(name, statusLabel2);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton launchButton = new JButton(resizeIcon(loadIcon("/resources/play.png"), 24, 24));
        launchButton.setFocusPainted(false);        // 포커스 표시 제거
        JButton editButton = new JButton(resizeIcon(loadIcon("/resources/edit.png"), 24, 24));
        editButton.setFocusPainted(false);        // 포커스 표시 제거

        // 실행 버튼 클릭 이벤트
        launchButton.addActionListener(e -> {
            if (runningServers.containsKey(name)) {
                // 서버가 실행 중이면 중지
                stopServer(name, launchButton);
            } else {
                // 서버 실행
                startServer(name, launchButton);
            }
        });

        // 수정 버튼 클릭 이벤트
        editButton.addActionListener(e -> openServerManageFrame(name, version));

        right.add(launchButton);
        right.add(editButton);

        box.add(left, BorderLayout.WEST);
        box.add(right, BorderLayout.EAST);

        return box;
    }

    // 서버 디렉토리를 가져오는 헬퍼 메서드 추가
    private static File getServerDirectory(String serverName) {
        String sanitizedName = sanitizeFileName(serverName);
        return new File("servers", sanitizedName);
    }

    // 로그 파일을 가져오는 헬퍼 메서드 추가
    private static File getLogFile(String serverName) {
        return new File(getServerDirectory(serverName), "console.log");
    }

    private static void openServerManageFrame(String serverName, String version) {
        // 이미 열린 창이 있으면 포커스만 주기
        if (serverManageFrames.containsKey(serverName)) {
            JFrame existingFrame = serverManageFrames.get(serverName);
            existingFrame.toFront();
            existingFrame.requestFocus();
            return;
        }

        JFrame manageFrame = new JFrame("서버 관리 - " + serverName);
        manageFrame.setSize(700, 500);
        manageFrame.setLocationRelativeTo(null);
        manageFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // 서버 폴더 열기 버튼 업데이트 - getServerDirectory 사용
        JButton openFolder = new JButton("폴더 열기");
        openFolder.setFocusPainted(false);
        openFolder.addActionListener(e -> {
            File serverFolder = getServerDirectory(serverName);
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
        });

        // 상단 정보 패널 - BorderLayout으로 변경
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBorder(BorderFactory.createTitledBorder("서버 정보"));

        // 왼쪽에 서버 정보 표시
        JPanel leftInfo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftInfo.add(new JLabel("" + serverName));
        leftInfo.add(new JLabel("     " + version));

        // 삭제 버튼
        JButton deleteButton = new JButton("서버 삭제");
        deleteButton.setFocusPainted(false);
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

        // 오른쪽에 두 버튼을 담을 패널 생성
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightPanel.add(openFolder);
        rightPanel.add(deleteButton);

        // 레이아웃에 추가
        infoPanel.add(leftInfo, BorderLayout.WEST);
        infoPanel.add(rightPanel, BorderLayout.EAST);

        // 콘솔 출력 영역
        JTextArea consoleArea = new JTextArea();
        consoleArea.setEditable(false);
        consoleArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        consoleArea.setBackground(Color.BLACK);
        consoleArea.setForeground(Color.WHITE);
        JScrollPane consoleScrollPane = new JScrollPane(consoleArea);
        consoleScrollPane.setBorder(BorderFactory.createTitledBorder("콘솔 출력"));
        consoleScrollPane.setPreferredSize(new Dimension(680, 300));

        // 수정: getLogFile 메서드 사용
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

        // 콘솔에 현재 상태 표시
        Process currentProcess = runningServers.get(serverName);
        if (currentProcess != null && currentProcess.isAlive()) {
        } else {
            consoleArea.append("서버가 중지되어 있습니다.\n");
        }

        serverConsoleAreas.put(serverName, consoleArea);

        // 명령어 입력 영역
        JPanel commandPanel = new JPanel(new BorderLayout());
        commandPanel.setBorder(BorderFactory.createTitledBorder("명령어"));

        JTextField commandField = new JTextField();
        commandField.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JButton sendButton = new JButton("전송");
        sendButton.setFocusPainted(false);

        ActionListener sendCommand = e -> {
            String command = commandField.getText().trim();
            if (!command.isEmpty()) {
                Process process = runningServers.get(serverName);
                if (process != null && process.isAlive()) {
                    try {
                        OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream());
                        writer.write(command + "\n");
                        writer.flush();
                        consoleArea.append("> " + command + "\n");
                        consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
                        commandField.setText("");
                    } catch (IOException ex) {
                        consoleArea.append("명령어 전송 실패: " + ex.getMessage() + "\n");
                    }
                } else {
                    consoleArea.append("서버가 실행 중이 아닙니다.\n");
                }
            }
        };

        commandField.addActionListener(sendCommand);
        sendButton.addActionListener(sendCommand);

        commandPanel.add(commandField, BorderLayout.CENTER);
        commandPanel.add(sendButton, BorderLayout.EAST);

        // 레이아웃 구성
        manageFrame.setLayout(new BorderLayout());
        manageFrame.add(infoPanel, BorderLayout.NORTH);
        manageFrame.add(consoleScrollPane, BorderLayout.CENTER);
        manageFrame.add(commandPanel, BorderLayout.SOUTH);

        // 창 닫을 때 정리
        manageFrame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                serverManageFrames.remove(serverName);
                serverConsoleAreas.remove(serverName);
            }
        });

        serverManageFrames.put(serverName, manageFrame);
        manageFrame.setVisible(true);
    }

    private static void deleteServer(String serverName) {
        // 실행 중인 서버 중지
        Process process = runningServers.get(serverName);
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
            runningServers.remove(serverName);
        }

        // 서버 폴더 삭제 - getServerDirectory 사용
        File serverDir = getServerDirectory(serverName);
        if (serverDir.exists()) {
            deleteDirectory(serverDir);
        }

        // 나머지 UI 정리 로직은 동일...
        JPanel serverBox = serverBoxes.get(serverName);
        if (serverBox != null && servers != null) {
            SwingUtilities.invokeLater(() -> {
                servers.removeServerBox(serverBox);
                serverBoxes.remove(serverName);

                JPanel serversPanel = servers.getServers();
                serversPanel.setPreferredSize(null);
                serversPanel.revalidate();
                serversPanel.repaint();

                JFrame mainFrame = (JFrame) SwingUtilities.getWindowAncestor(serverBox);
                if (mainFrame != null) {
                    mainFrame.revalidate();
                    mainFrame.repaint();
                }
            });
        }

        // 관련 데이터 정리
        serverStatusLabels.remove(serverName);
        serverStatusLabels2.remove(serverName);
        serverConsoleAreas.remove(serverName);

        // 관리 창 닫기
        JFrame manageFrame = serverManageFrames.get(serverName);
        if (manageFrame != null) {
            manageFrame.dispose();
            serverManageFrames.remove(serverName);
        }

        // 성공 메시지
        JOptionPane.showMessageDialog(null,
                "서버 '" + serverName + "'이(가) 삭제되었습니다.",
                "서버 삭제 완료",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private static void deleteDirectory(File directory) {
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

    private static void startServer(String serverName, JButton launchButton) {
        File serverDir = getServerDirectory(serverName);
        File paperJar = new File(serverDir, "paper.jar");

        if (!paperJar.exists()) {
            updateServerStatus(serverName, "paper.jar 파일이 없습니다", "");
            return;
        }

        try {
            // ✅ 로그 초기화: 서버 시작 전에 console.log 파일을 비움 - getLogFile 사용
            File logFile = getLogFile(serverName);
            if (logFile.exists()) {
                new PrintWriter(logFile).close(); // 파일 내용 비움
            }

            ProcessBuilder pb = new ProcessBuilder("java", "-jar", "paper.jar", "nogui");
            pb.directory(serverDir);
            pb.redirectErrorStream(true);

            Process process = pb.start();
            runningServers.put(serverName, process);

            // ✅ 수정: 서버 시작 시 버튼을 정지 버튼으로 변경
            SwingUtilities.invokeLater(() -> {
                launchButton.setIcon(resizeIcon(loadIcon("/resources/stop.png"), 24, 24));
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
                // 서버에 stop 명령 전송
                OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream());
                writer.write("stop\n");
                writer.flush();

                // 5초 대기 후 강제 종료
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
        // 수정: getLogFile 메서드 사용
        File logFile = getLogFile(serverName);

        try (
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                BufferedWriter logWriter = new BufferedWriter(new FileWriter(logFile, true))
        ) {
            String line;
            String[] lastTwoLines = {"", ""};

            while ((line = reader.readLine()) != null && process.isAlive()) {
                lastTwoLines[0] = lastTwoLines[1];
                lastTwoLines[1] = line;

                // 로그 저장
                logWriter.write(line + "\n");
                logWriter.flush();

                final String finalLine = line;
                final String[] finalLastTwoLines = lastTwoLines.clone();
                SwingUtilities.invokeLater(() -> {
                    String cleanLine1 = cleanLogLine(finalLastTwoLines[0]);
                    String cleanLine2 = cleanLogLine(finalLastTwoLines[1]);
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
                launchButton.setIcon(resizeIcon(loadIcon("/resources/play.png"), 24, 24));
                launchButton.setText("");
                launchButton.setToolTipText("서버 시작");
                updateServerStatus(serverName, "서버 중지됨", "");

                JTextArea consoleArea = serverConsoleAreas.get(serverName);
                if (consoleArea != null) {
                    consoleArea.append("서버가 중지되었습니다.\n");
                    consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
                }
            });

        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> {
                updateServerStatus(serverName, "출력 읽기 실패", "");
            });
        }
    }

    private static String cleanLogLine(String line) {
        if (line == null || line.isEmpty()) return "";

        // 마인크래프트 서버 로그에서 불필요한 정보 제거
        if (line.contains("]: ")) {
            int index = line.indexOf("]: ");
            if (index != -1 && index + 3 < line.length()) {
                String cleanedLine = line.substring(index + 3);
                // 길이 제한 (UI에 맞게)
                if (cleanedLine.length() > 50) {
                    cleanedLine = cleanedLine.substring(0, 47) + "...";
                }
                return cleanedLine;
            }
        }

        // 길이 제한
        if (line.length() > 50) {
            line = line.substring(0, 47) + "...";
        }

        return line;
    }

    private static void updateServerStatus(String serverName, String status1, String status2) {
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

    public static ImageIcon loadIcon(String path) {
        try {
            return new ImageIcon(Objects.requireNonNull(Basket.class.getResource(path)));
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

    public static void saveServerConfig(String name, String version, String launcher, String eula) {
        // Sanitize the name to remove invalid characters for Windows file system
        String sanitizedName = sanitizeFileName(name);

        File serverDir = new File("servers", sanitizedName);
        if (!serverDir.exists()) serverDir.mkdirs();

        File settingsFile = new File(serverDir, "settings.txt");

        try (PrintWriter writer = new PrintWriter(settingsFile)) {
            writer.println("name=" + name); // Keep original name in settings
            writer.println("sanitized_name=" + sanitizedName); // Store sanitized name for reference
            writer.println("version=" + version);
            writer.println("launcher=" + launcher);
            writer.println("eula=" + eula);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Paper 실행기 다운로드
        if ("paper".equalsIgnoreCase(launcher)) {
            try {
                downloadLatestPaperJar(version, new File(serverDir, "paper.jar"));

                // EULA 파일 생성
                createEulaFile(serverDir, eula);

            } catch (Exception e) {
                System.err.println("Paper 실행기 다운로드 실패: " + e.getMessage());
            }
        }
    }

    /**
     * Sanitizes a filename by replacing invalid characters with underscores
     * @param fileName the original filename
     * @return sanitized filename safe for Windows file system
     */
    private static String sanitizeFileName(String fileName) {
        if (fileName == null) return "server";

        // Windows와 일부 OS에서 허용되지 않는 문자 목록:
        // \ / : * ? " < > | 및 제어 문자, ! 등
        String sanitized = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");

        // 제어 문자 (0x00~0x1F) 제거
        sanitized = sanitized.replaceAll("[\\x00-\\x1F]", "_");

        // 끝에 오는 공백 및 마침표 제거 (Windows 제한사항)
        sanitized = sanitized.replaceAll("[.\\s]+$", "");

        // 이름이 비었거나 예약된 이름일 경우 대체
        if (sanitized.isEmpty() || isReservedName(sanitized)) {
            sanitized = "server_" + System.currentTimeMillis();
        }

        // 최대 길이 제한 (일반적으로 255자 이하, 안전하게 100자 제한)
        if (sanitized.length() > 100) {
            sanitized = sanitized.substring(0, 100);
        }

        return sanitized;
    }

    /**
     * Checks if a name is reserved in Windows
     */
    private static boolean isReservedName(String name) {
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

    private static void createEulaFile(File serverDir, String eulaAccepted) {
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

        JSONObject latestBuildObj = builds.getJSONObject(builds.length() - 1); // 마지막 = 최신
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

    public static void loadSavedServers(Servers servers) {
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
                    servers.addServerBox(serverBox);
                    serverBoxes.put(name, serverBox); // 서버 박스 추적
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