import ServerWizard.ServerWizard;
import Settings.Settings;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.util.concurrent.ConcurrentHashMap;

public class Basket extends JFrame {
    // 데이터 저장소들
    public static JButton addButton;
    public static JButton settings;

    private static JPanel serversPanel;
    private static ConcurrentHashMap<String, Process> runningServers = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, JLabel> serverStatusLabels = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, JLabel> serverStatusLabels2 = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, JTextArea> serverConsoleAreas = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, JFrame> serverManageFrames = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, JPanel> serverBoxes = new ConcurrentHashMap<>();
    private static JFrame settingsFrame = null;

    public static void main(String[] args) {
        initializeMainComponents();
        loadExistingServers();
        createAndShowMainWindow();
        setupApplicationShutdown();
    }



    // ===== 메인 컴포넌트 초기화 =====

    private static void initializeMainComponents() {
        // 서버들을 담을 패널 생성
        serversPanel = new JPanel();
        serversPanel.setLayout(new BoxLayout(serversPanel, BoxLayout.Y_AXIS));
        serversPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        serversPanel.add(Box.createVerticalGlue());
    }

    private static void loadExistingServers() {
        // utils의 로딩 블록을 사용해서 저장된 서버들 불러오기
        utils.loadAndCreateSavedServers(
                serversPanel,
                serverBoxes,
                serverStatusLabels,
                serverStatusLabels2,
                e -> toggleServerState(getServerNameFromButton((JButton) e.getSource())),
                e -> openServerManageFrame(getServerNameFromButton((JButton) e.getSource()))
        );
    }

    private static void createAndShowMainWindow() {
        JPanel navigationPanel = utils.createNavigationPanel(600, 50);

        addButton = utils.createTextButton(100, 40, "서버 추가");
        addButton.addActionListener(e -> showServerCreationWizard());
        navigationPanel.add(addButton);

        settings = utils.createImageButton(40, 40, "resources/settings.png");
        settings.addActionListener(e -> openSettings());
        navigationPanel.add(settings);

        // 스크롤 패널 생성 (utils의 스크롤 블록 사용)
        JScrollPane scrollPane = utils.createScrollPane(serversPanel, false, true);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setPreferredSize(new Dimension(580, 0));

        // 메인 프레임 생성 (utils의 프레임 블록 사용)
        JFrame mainFrame = utils.createMainFrame("Basket", 600, 400, navigationPanel, scrollPane);
        mainFrame.setVisible(true);
    }

    private static void setupApplicationShutdown() {
        // utils의 셧다운 훅 블록 사용
        utils.setupShutdownHook(runningServers);
    }



    // ===== 서버 생성 마법사 =====

    private static void showServerCreationWizard() {
        addButton.setEnabled(false);

        ServerWizard wizard = new ServerWizard();
        wizard.showWizard((name, version, launcher, agreedEula) -> {
            // utils의 설정 저장 블록 사용
            utils.saveServerConfig(name, version, launcher, agreedEula.toString());

            // utils의 서버 박스 생성 블록 사용
            JPanel serverBox = utils.createServerBox(
                    name, version,
                    serverStatusLabels, serverStatusLabels2,
                    e -> toggleServerState(name),
                    e -> openServerManageFrame(name)
            );

            // 패널에 추가 (글루 전에 삽입)
            int insertIndex = serversPanel.getComponentCount() - 1; // 글루 전에
            serversPanel.add(serverBox, insertIndex);
            serversPanel.add(Box.createRigidArea(new Dimension(0, 8)), insertIndex + 1);
            serversPanel.revalidate();
            serversPanel.repaint();
            serverBoxes.put(name, serverBox);

            addButton.setEnabled(true);
        }, () -> {
            // 창이 닫힐 때 addButton 다시 활성화
            addButton.setEnabled(true);
        });
    }



    // ===== Basket 설정 =====

    private static void openSettings() {
        // 이미 열린 설정 창이 있으면 포커스만 이동
        if (settingsFrame != null && settingsFrame.isDisplayable()) {
            settingsFrame.toFront();
            settingsFrame.requestFocus();
            return;
        }

        Settings settings1 = new Settings();
        settingsFrame = settings1.showSettings();
        
        // 창이 닫힐 때 참조 제거
        if (settingsFrame != null) {
            settingsFrame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    settingsFrame = null;
                }
            });
        }
    }



    // ===== 서버 상태 토글 =====

    private static void toggleServerState(String serverName) {
        if (runningServers.containsKey(serverName)) {
            stopServer(serverName);
        } else {
            startServer(serverName);
        }
    }

    private static void startServer(String serverName) {
        try {
            // utils의 프로세스 시작 블록 사용
            Process process = utils.startServerProcess(serverName);
            runningServers.put(serverName, process);

            // UI 업데이트
            SwingUtilities.invokeLater(() -> {
                updateServerButtonIcon(serverName, "/resources/stop.png", "서버 중지");
                updateServerStatus(serverName, "서버 시작 중...", "");
            });

            // utils의 출력 모니터링 블록 사용
            utils.monitorServerOutput(
                    serverName, process,
                    serverStatusLabels, serverStatusLabels2, serverConsoleAreas,
                    () -> onServerStopped(serverName)
            );

        } catch (IOException e) {
            updateServerStatus(serverName, "실행 실패: " + e.getMessage(), "");
            e.printStackTrace();
        }
    }

    private static void stopServer(String serverName) {
        Process process = runningServers.get(serverName);
        if (process != null) {
            // utils의 프로세스 중지 블록 사용
            utils.stopServerProcess(process);
        }
    }

    private static void onServerStopped(String serverName) {
        // 서버가 중지되었을 때의 UI 업데이트
        runningServers.remove(serverName);
        updateServerButtonIcon(serverName, "/resources/play.png", "서버 시작");
        updateServerStatus(serverName, "서버 중지됨", "");

        JTextArea consoleArea = serverConsoleAreas.get(serverName);
        if (consoleArea != null) {
            consoleArea.append("서버가 중지되었습니다.\n");
            consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
        }
    }



    // ===== 서버 관리 창 =====

    private static void openServerManageFrame(String serverName) {
        // 이미 열린 창이 있으면 포커스만 이동
        if (serverManageFrames.containsKey(serverName)) {
            JFrame existingFrame = serverManageFrames.get(serverName);
            existingFrame.toFront();
            existingFrame.requestFocus();
            return;
        }

        // 서버 정보 가져오기
        String version = getServerVersion(serverName);

        // utils의 관리 프레임 생성 블록 사용
        JFrame manageFrame = utils.createServerManageFrame(serverName, version);

        // utils의 각종 패널 생성 블록들 사용
        JPanel infoPanel = utils.createServerInfoManagePanel(
                serverName, version,
                e -> utils.openServerFolder(serverName, manageFrame),
                e -> deleteServerWithConfirmation(serverName, manageFrame)
        );

        JScrollPane consoleScrollPane = utils.createConsolePanel(serverName, serverConsoleAreas, runningServers);
        JPanel commandPanel = utils.createCommandPanel(serverName, runningServers, serverConsoleAreas);

        // 레이아웃 조합
        manageFrame.setLayout(new BorderLayout());
        manageFrame.add(infoPanel, BorderLayout.NORTH);
        manageFrame.add(consoleScrollPane, BorderLayout.CENTER);
        manageFrame.add(commandPanel, BorderLayout.SOUTH);

        // 창 닫힐 때 정리 작업
        manageFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                serverManageFrames.remove(serverName);
                serverConsoleAreas.remove(serverName);
            }
        });

        serverManageFrames.put(serverName, manageFrame);
        manageFrame.setVisible(true);
    }



    // ===== 서버 삭제 =====

    private static void deleteServerWithConfirmation(String serverName, JFrame parentFrame) {
        // utils의 확인 대화상자 블록 사용
        if (utils.confirmServerDeletion(serverName, parentFrame)) {
            // utils의 완전 삭제 블록 사용
            utils.deleteServerCompletely(
                    serverName,
                    runningServers,
                    serverStatusLabels, serverStatusLabels2,
                    serverConsoleAreas, serverManageFrames,
                    serverBoxes, serversPanel
            );
            parentFrame.dispose();
        }
    }



    // ===== 유틸리티 메서드들 =====

    private static void updateServerStatus(String serverName, String status1, String status2) {
        utils.updateServerStatus(serverName, status1, status2, serverStatusLabels, serverStatusLabels2);
    }

    private static void updateServerButtonIcon(String serverName, String iconPath, String tooltip) {
        JPanel serverBox = serverBoxes.get(serverName);
        if (serverBox != null) {
            // 서버 박스에서 버튼 찾아서 아이콘 업데이트
            Component[] components = ((JPanel) serverBox.getComponent(1)).getComponents();
            for (Component comp : components) {
                if (comp instanceof JButton) {
                    JButton button = (JButton) comp;
                    if (button.getIcon() != null) { // 아이콘이 있는 버튼 (launch 버튼)
                        button.setIcon(utils.resizeIcon(utils.loadIcon(iconPath), 24, 24));
                        button.setToolTipText(tooltip);
                        break;
                    }
                }
            }
        }
    }

    private static String getServerNameFromButton(JButton button) {
        // 버튼으로부터 서버 이름을 찾는 로직
        // 현재 구조에서는 별도 방법이 필요하므로 임시 구현
        Container parent = button.getParent();
        while (parent != null && !(parent instanceof JPanel && serverBoxes.containsValue(parent))) {
            parent = parent.getParent();
        }

        if (parent != null) {
            for (String name : serverBoxes.keySet()) {
                if (serverBoxes.get(name) == parent) {
                    return name;
                }
            }
        }
        return "";
    }

    private static String getServerVersion(String serverName) {
        File serverDir = utils.getServerDirectory(serverName);
        File settingsFile = new File(serverDir, "settings.txt");

        if (settingsFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(settingsFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("version=")) {
                        return line.substring(8);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "알 수 없음";
    }



    // 서버 생성 콜백 인터페이스
    public interface ServerCreationCallback {
        void onServerCreated(String name, String version, String launcher, Boolean eula);
    }

}