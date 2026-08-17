package org.ljcode.myPlugin.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fi.iki.elonen.NanoHTTPD;
import org.ljcode.myPlugin.MyPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class WebServer extends NanoHTTPD {
    private final MyPlugin plugin;
    private final Gson gson;
    private final String webRoot;

    public WebServer(MyPlugin plugin, int port) throws IOException {
        super(port);
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        // 将web文件夹复制到插件数据文件夹中
        this.webRoot = plugin.getDataFolder().getAbsolutePath() + "/web";
        copyWebFiles();
    }

    private void copyWebFiles() {
        File webDir = new File(webRoot);
        if (!webDir.exists()) {
            webDir.mkdirs();
        }

        // 从插件jar外部的web目录复制文件到插件数据文件夹
        // 首先尝试在插件目录的同级目录查找web文件夹
        File pluginsDir = plugin.getDataFolder().getParentFile(); // plugins目录
        File serverRootDir = pluginsDir.getParentFile(); // 服务器根目录
        File sourceWebDir = new File(serverRootDir, "web");
        
        // 如果上面路径不存在，则尝试在插件jar所在目录下查找
        if (!sourceWebDir.exists()) {
            sourceWebDir = new File(plugin.getDataFolder().getParentFile(), "web");
        }
        
        // 最后尝试在项目开发目录（当前工作目录）下查找
        if (!sourceWebDir.exists()) {
            sourceWebDir = new File("web");
        }
        
        if (sourceWebDir.exists()) {
            // 复制web目录中的文件
            copyDirectory(sourceWebDir, webDir);
            plugin.getLogger().info("Web files copied from: " + sourceWebDir.getAbsolutePath() + " to: " + webRoot);
        } else {
            // 尝试从JAR资源复制（如果web目录在resources中）
            plugin.getLogger().info("Looking for web files in JAR resources...");
            copyWebFilesFromResources();
        }
    }

    private void copyDirectory(File sourceDir, File targetDir) {
        try {
            java.nio.file.Path targetPath = targetDir.toPath();
            java.nio.file.Path sourcePath = sourceDir.toPath();
            
            java.nio.file.Files.walk(sourcePath)
                .forEach(source -> {
                    try {
                        java.nio.file.Path target = targetPath.resolve(sourcePath.relativize(source));
                        if (source.toFile().isDirectory()) {
                            java.nio.file.Files.createDirectories(target);
                        } else {
                            java.nio.file.Files.copy(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (IOException e) {
                        plugin.getLogger().severe("Error copying file: " + e.getMessage());
                    }
                });
        } catch (IOException e) {
            plugin.getLogger().severe("Error walking through source directory: " + e.getMessage());
        }
    }

    private void copyWebFilesFromResources() {
        try {
            // 直接调用提取资源文件的方法
            extractWebFilesFromResources();
        } catch (Exception e) {
            plugin.getLogger().severe("Error accessing JAR resources: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void extractWebFilesFromResources() {
        try {
            // 获取JAR文件
            java.net.URL jarUrl = getClass().getProtectionDomain().getCodeSource().getLocation();
            java.nio.file.Path jarPath = java.nio.file.Paths.get(jarUrl.toURI());
            
            if (java.nio.file.Files.exists(jarPath) && jarPath.toString().endsWith(".jar")) {
                // 从JAR中提取web文件
                try (java.util.jar.JarFile jarFile = new java.util.jar.JarFile(jarPath.toFile())) {
                    java.util.Enumeration<java.util.jar.JarEntry> entries = jarFile.entries();
                    
                    while (entries.hasMoreElements()) {
                        java.util.jar.JarEntry entry = entries.nextElement();
                        
                        if (entry.getName().startsWith("web/") && !entry.isDirectory()) {
                            // 提取web目录下的文件，去掉"web/"前缀，直接放到webRoot目录下
                            String relativePath = entry.getName().substring("web/".length());
                            java.io.File targetFile = new java.io.File(webRoot, relativePath);
                            
                            // 确保父目录存在
                            targetFile.getParentFile().mkdirs();
                            
                            // 提取文件内容
                            try (java.io.InputStream inputStream = jarFile.getInputStream(entry);
                                 java.io.FileOutputStream outputStream = new java.io.FileOutputStream(targetFile)) {
                                
                                byte[] buffer = new byte[4096];
                                int bytesRead;
                                while ((bytesRead = inputStream.read(buffer)) != -1) {
                                    outputStream.write(buffer, 0, bytesRead);
                                }
                            }
                        }
                    }
                }
                plugin.getLogger().info("Web files extracted from JAR to: " + webRoot);
            } else {
                // 开发环境 - 从源码目录复制
                java.io.File sourceDir = new java.io.File("web");
                if (sourceDir.exists() && sourceDir.isDirectory()) {
                    copyDirectory(sourceDir, new java.io.File(webRoot));
                    plugin.getLogger().info("Web files copied from development directory to: " + webRoot);
                } else {
                    plugin.getLogger().warning("No web files found in JAR resources or web directory.");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error extracting web files from resources: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Method method = session.getMethod();

        // 处理API请求
        if (uri.startsWith("/api/")) {
            return handleApiRequest(session, method, uri);
        }

        // 处理静态文件请求
        return handleStaticFile(uri);
    }

    private Response handleApiRequest(IHTTPSession session, Method method, String uri) {
        // 认证检查：IP白名单 + 可选token（静态页面不受影响）
        if (!isAuthorized(session)) {
            return newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT,
                    "Access denied: IP not allowed or invalid token");
        }

        if ("/api/config".equals(uri)) {
            if (method == Method.GET) {
                return handleGetConfig();
            } else if (method == Method.POST) {
                return handlePostConfig(session);
            }
        }

        return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "API endpoint not found");
    }

    /**
     * API访问认证：
     * 1. IP白名单（web.allowed-ips，支持 * 结尾的通配前缀，如 192.168.*）
     * 2. 可选token（web.api-token 非空时，请求必须带 ?token=xxx 或 X-API-Token 头）
     * @param session HTTP会话
     * @return 是否放行
     */
    private boolean isAuthorized(IHTTPSession session) {
        // 1. IP 白名单检查
        String clientIp = session.getRemoteIpAddress();
        java.util.List<String> allowedIps = plugin.getConfig().getStringList("web.allowed-ips");
        if (allowedIps == null || allowedIps.isEmpty()) {
            // 未配置时默认仅允许本机访问，避免局域网任意主机篡改服务器配置
            allowedIps = java.util.Arrays.asList("127.0.0.1", "::1", "localhost");
        }

        boolean ipAllowed = false;
        for (String pattern : allowedIps) {
            if (matchesIpPattern(clientIp, pattern)) {
                ipAllowed = true;
                break;
            }
        }
        if (!ipAllowed) {
            plugin.getLogger().warning("[Web] 拒绝来自未授权IP的API请求: " + clientIp);
            return false;
        }

        // 2. 可选token检查（未配置token时跳过）
        String requiredToken = plugin.getConfig().getString("web.api-token", "");
        if (requiredToken != null && !requiredToken.isEmpty()) {
            String provided = session.getParms().get("token");
            if (provided == null) {
                provided = session.getHeaders().get("x-api-token");
            }
            if (provided == null || !provided.equals(requiredToken)) {
                plugin.getLogger().warning("[Web] API token 校验失败，IP: " + clientIp);
                return false;
            }
        }
        return true;
    }

    /**
     * IP匹配：支持精确匹配和 * 结尾的前缀通配（如 192.168.*）
     */
    private boolean matchesIpPattern(String clientIp, String pattern) {
        if (pattern == null || clientIp == null) {
            return false;
        }
        if (pattern.endsWith("*")) {
            return clientIp.startsWith(pattern.substring(0, pattern.length() - 1));
        }
        return pattern.equals(clientIp);
    }

    private Response handleGetConfig() {
        try {
            File configFile = new File(plugin.getDataFolder(), "config.yml");
            YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            
            // 将YAML配置转换为Map
            Map<String, Object> configMap = new HashMap<>();
            for (String key : config.getKeys(true)) {
                configMap.put(key, config.get(key));
            }
            
            String jsonResponse = gson.toJson(configMap);
            return newFixedLengthResponse(Response.Status.OK, "application/json", jsonResponse);
        } catch (Exception e) {
            plugin.getLogger().severe("Error reading config: " + e.getMessage());
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Error reading config");
        }
    }

    private Response handlePostConfig(IHTTPSession session) {
        try {
            // 解析请求体
            java.util.Map<String, String> files = new java.util.HashMap<>();
            session.parseBody(files);

            String postData = "";

            // 尝试从临时文件获取数据（multipart/form-data）
            if (!files.isEmpty()) {
                String tempFileName = files.values().iterator().next();
                File tempFile = new File(tempFileName);
                postData = java.nio.file.Files.readString(tempFile.toPath());
            } else {
                // 尝试从参数获取数据
                java.util.Map<String, String> parms = session.getParms();
                for (String value : parms.values()) {
                    if (value.startsWith("{") && value.endsWith("}")) { // 可能是JSON数据
                        postData = value;
                        break;
                    }
                }
            }

            // 如果以上方法都没获取到数据，尝试直接读取输入流
            if (postData.isEmpty()) {
                try {
                    java.io.InputStream inputStream = session.getInputStream();
                    java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
                    int nRead;
                    byte[] data = new byte[1024];
                    while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                        buffer.write(data, 0, nRead);
                    }
                    postData = new String(buffer.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
                } catch (Exception e) {
                    // 如果直接读取输入流失败，返回错误
                    return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Failed to read request body");
                }
            }

            if (postData.isEmpty()) {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "No data received");
            }

            // 确保获得的是有效的JSON
            postData = postData.trim();
            if (!postData.startsWith("{") && !postData.endsWith("}")) {
                // 如果参数看起来像URL编码的JSON，检查参数键
                java.util.Map<String, String> parms = session.getParms();
                if (!parms.isEmpty()) {
                    // 检查是否有类似JSON的内容
                    for (String value : parms.values()) {
                        if (value.startsWith("{") && value.endsWith("}")) {
                            postData = value;
                            break;
                        }
                    }
                }
            }

            Map<String, Object> configMap = gson.fromJson(postData, Map.class);

            // 保存配置到config.yml
            File configFile = new File(plugin.getDataFolder(), "config.yml");
            YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

            // 只更新提交的配置项，不清空现有配置
            setConfigValues(config, configMap, "");

            config.save(configFile);

            // 重新加载插件配置
            plugin.reloadConfig();

            // 同步新配置对象到K10链路各持有者，确保Web修改的K10设置立即生效
            plugin.syncK10Config();

            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"status\":\"success\",\"message\":\"Config saved successfully\"}");
        } catch (Exception e) {
            plugin.getLogger().severe("Error saving config: " + e.getMessage());
            e.printStackTrace(); // 添加详细错误日志
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Error saving config: " + e.getMessage());
        }
    }

    // 递归设置配置值
    private void setConfigValues(YamlConfiguration config, Map<String, Object> map, String prefix) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof Map) {
                // 递归处理嵌套映射
                @SuppressWarnings("unchecked")
                Map<String, Object> subMap = (Map<String, Object>) value;
                setConfigValues(config, subMap, key);
            } else {
                config.set(key, value);
            }
        }
    }

    private Response handleStaticFile(String uri) {
        // 默认返回index.html
        if ("/".equals(uri)) {
            uri = "/index.html";
        }

        // 构造文件路径
        String filePath = webRoot + uri;
        File file = new File(filePath);

        // 安全检查，防止路径遍历
        String canonicalPath;
        try {
            canonicalPath = file.getCanonicalPath();
            String canonicalWebRoot = new File(webRoot).getCanonicalPath();
            if (!canonicalPath.startsWith(canonicalWebRoot)) {
                return newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "Access denied");
            }
        } catch (IOException e) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Error accessing file");
        }

        if (!file.exists() || !file.isFile()) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "File not found");
        }

        // 根据文件扩展名确定MIME类型
        String mimeType = getMimeType(file.getName());

        try {
            byte[] fileContent = java.nio.file.Files.readAllBytes(file.toPath());
            // 使用适当的响应方法处理二进制文件
            if(isBinaryFile(mimeType)) {
                // 为二进制文件使用流式响应
                return newFixedLengthResponse(Response.Status.OK, mimeType, java.nio.file.Files.newInputStream(file.toPath()), fileContent.length);
            } else {
                // 文本文件正常处理
                return newFixedLengthResponse(Response.Status.OK, mimeType, new String(fileContent, "UTF-8"));
            }
        } catch (IOException e) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Error reading file");
        }
    }

    private String getMimeType(String fileName) {
        if (fileName.endsWith(".html") || fileName.endsWith(".htm")) {
            return "text/html";
        } else if (fileName.endsWith(".css")) {
            return "text/css";
        } else if (fileName.endsWith(".js")) {
            return "application/javascript";
        } else if (fileName.endsWith(".json")) {
            return "application/json";
        } else if (fileName.endsWith(".png")) {
            return "image/png";
        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (fileName.endsWith(".gif")) {
            return "image/gif";
        } else {
            return "application/octet-stream";
        }
    }

    private boolean isBinaryFile(String mimeType) {
        return mimeType.startsWith("image/") || 
               mimeType.startsWith("audio/") || 
               mimeType.startsWith("video/") ||
               mimeType.equals("application/octet-stream");
    }

    public void startServer() {
        try {
            start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            plugin.getLogger().info("Web server started on port " + getListeningPort());
        } catch (IOException e) {
            plugin.getLogger().severe("Could not start web server: " + e.getMessage());
        }
    }
}