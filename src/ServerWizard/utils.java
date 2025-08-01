package ServerWizard;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.List;
import org.json.*;

public class utils {

    // ===== UI 컴포넌트 생성 블록들 =====

    public static JButton createTextButton(Integer width, Integer height, String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(width, height));
        return button;
    }

    public static JToggleButton createLauncherButton(String name, String imagePath) {
        JToggleButton button = new JToggleButton();
        button.setPreferredSize(new Dimension(120, 120));
        button.setLayout(new BorderLayout());
        button.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        button.setFocusPainted(false);

        JLabel imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);

        try {
            ImageIcon icon = new ImageIcon(utils.class.getResource(imagePath));
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
        String sanitizedName = sanitizeFileName(serverName);
        return new File("servers", sanitizedName);
    }

    // ===== 서버 관리 관련 유틸리티 =====

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
}