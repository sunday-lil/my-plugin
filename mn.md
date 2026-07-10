/*!
* Minecraft × K10 联动接收器 (聊天室UI版) - 美化版
* 修正底部文字超出屏幕问题，修复 blinkColor 作用域错误
* 新增完整环境数据显示功能
  */
  #include "unihiker_k10.h"
  #include <WiFi.h>
  #include <WebServer.h>
  #include <Preferences.h>
  #include <SD.h>

// ================= 全局对象 =================
UNIHIKER_K10 k10;
WebServer server(80);
Preferences prefs;

bool needApConfig = false;
String deviceIP = "";

String cachedSsidList = "";
bool scanDone = false;

// ---------- 聊天消息系统 ----------
#define MAX_MSG 8
String msgList[MAX_MSG];
int msgCount = 0;
int msgHead = 0;

File logFile;
bool sdOk = false;

// ---------- 状态栏自动恢复 ----------
String defaultStatusLine1 = "";
String defaultStatusLine2 = "";
uint32_t defaultColor1 = 0x00FFAA;
uint32_t defaultColor2 = 0xCCCCCC;

String tempStatusLine1 = "";
String tempStatusLine2 = "";
uint32_t tempColor1 = 0xFFFFFF;
uint32_t tempColor2 = 0x00FFAA;
unsigned long statusChangeTime = 0;
bool isTempStatus = false;
const unsigned long STATUS_DISPLAY_DURATION = 5000;

// ============= 全局 LED 闪烁函数 =============
void blinkLED(uint32_t color) {
k10.rgb->write(-1, color);
delay(200);
k10.rgb->write(-1, 0x000000);
delay(100);
k10.rgb->write(-1, color);
delay(200);
k10.rgb->write(-1, 0x000000);
}

// ---------- 屏幕工具 ----------
void drawUI(String statusLine1, String statusLine2, uint32_t statusColor1 = 0x00FFAA, uint32_t statusColor2 = 0xCCCCCC) {
k10.canvas->canvasClear();
k10.setScreenBackground(0x0A0A1A);

    // 顶部状态区域
    k10.canvas->canvasText(statusLine1, 10, 10, statusColor1, k10.canvas->eCNAndENFont16, 50, true);
    k10.canvas->canvasText(statusLine2, 10, 38, statusColor2, k10.canvas->eCNAndENFont16, 45, true);
    
    // 状态指示点
    k10.canvas->canvasCircle(220, 20, 6, statusColor1, statusColor1, true);
    
    // 消息列表标题
    k10.canvas->canvasText("== 消息记录 ==", 10, 70, 0x6688AA, k10.canvas->eCNAndENFont16, 30, true);
    
    // 消息列表
    int y = 92;
    for (int i = 0; i < msgCount; i++) {
        int idx = (msgHead - i + MAX_MSG) % MAX_MSG;
        if (msgList[idx].length() > 0) {
            String display = msgList[idx];
            uint32_t msgColor = 0xCCCCCC;
            if (display.startsWith("[加入]")) msgColor = 0x66FF66;
            else if (display.startsWith("[离开]")) msgColor = 0xFFAA44;
            else if (display.startsWith("[阵亡]")) msgColor = 0xFF4444;
            else if (display.startsWith("[消息]")) msgColor = 0x44DDFF;
            else if (display.startsWith("[系统]")) msgColor = 0xAA88FF;
            else if (display.startsWith("[环境]")) msgColor = 0x00AAFF;
            
            if (display.length() > 22) {
                display = display.substring(0, 22) + "...";
            }
            k10.canvas->canvasText(display, 12, y, msgColor, k10.canvas->eCNAndENFont16, 38, true);
            y += 26;
        }
    }
    
    // 底部装饰文字 —— 上移避免超出屏幕
    k10.canvas->canvasText("◆ MC Bridge v2.0 ◆", 60, 288, 0x334466, k10.canvas->eCNAndENFont16, 25, true);
    
    k10.canvas->updateCanvas();
}

// ============= 环境数据专用显示UI =============
void drawEnvironmentUI(String playerName, String temperature, String humidity, String light, String windSpeed, String weather, String biome) {
k10.canvas->canvasClear();
k10.setScreenBackground(0x0A0A1A);

    // 标题区域
    k10.canvas->canvasText("🌍 环境数据监控", 10, 10, 0x00AAFF, k10.canvas->eCNAndENFont16, 50, true);
    k10.canvas->canvasText("玩家: " + playerName, 10, 35, 0xFFFFFF, k10.canvas->eCNAndENFont16, 45, true);
    
    // 分隔线
    k10.canvas->canvasLine(10, 60, 220, 60, 0x334466);
    
    // 温度显示（带具体数值）
    k10.canvas->canvasText("🌡️ 温度:", 15, 75, 0xFFAA44, k10.canvas->eCNAndENFont16, 30, true);
    k10.canvas->canvasText(temperature + "°C", 120, 75, 0xFFAA44, k10.canvas->eCNAndENFont16, 30, true);
    
    // 湿度显示（带具体数值）
    k10.canvas->canvasText("💧 湿度:", 15, 100, 0x44DDFF, k10.canvas->eCNAndENFont16, 30, true);
    k10.canvas->canvasText(humidity + "%", 120, 100, 0x44DDFF, k10.canvas->eCNAndENFont16, 30, true);
    
    // 光照显示（带具体数值）
    k10.canvas->canvasText("☀️ 光照:", 15, 125, 0xFFDD44, k10.canvas->eCNAndENFont16, 30, true);
    k10.canvas->canvasText(light, 120, 125, 0xFFDD44, k10.canvas->eCNAndENFont16, 30, true);
    
    // 风速显示（带具体数值）
    k10.canvas->canvasText("💨 风速:", 15, 150, 0x66FF66, k10.canvas->eCNAndENFont16, 30, true);
    k10.canvas->canvasText(windSpeed + "m/s", 120, 150, 0x66FF66, k10.canvas->eCNAndENFont16, 30, true);
    
    // 天气显示（带具体数值和中文转换）
    k10.canvas->canvasText("🌤️ 天气:", 15, 175, 0xAA88FF, k10.canvas->eCNAndENFont16, 30, true);
    String weatherDisplay = weather;
    if (weather == "clear") weatherDisplay = "晴朗";
    else if (weather == "storm") weatherDisplay = "暴雨";
    else if (weather == "thunder") weatherDisplay = "雷暴";
    k10.canvas->canvasText(weatherDisplay, 120, 175, 0xAA88FF, k10.canvas->eCNAndENFont16, 30, true);
    
    // 生物群系显示（截断过长的名称）
    k10.canvas->canvasText("🌿 群系:", 15, 200, 0x00FFAA, k10.canvas->eCNAndENFont16, 30, true);
    String biomeDisplay = biome;
    if (biomeDisplay.length() > 12) {
        biomeDisplay = biomeDisplay.substring(0, 12);
    }
    k10.canvas->canvasText(biomeDisplay, 120, 200, 0x00FFAA, k10.canvas->eCNAndENFont16, 30, true);
    
    // 底部装饰
    k10.canvas->canvasText("◆ 实时监控 ◆", 60, 288, 0x334466, k10.canvas->eCNAndENFont16, 25, true);
    
    k10.canvas->updateCanvas();
}

// 添加一条消息到队列
void addMessage(String msg) {
if (sdOk) {
logFile = SD.open("/log.txt", FILE_APPEND);
if (logFile) {
logFile.println(msg);
logFile.close();
}
}
msgList[msgHead] = msg;
msgHead = (msgHead + 1) % MAX_MSG;
if (msgCount < MAX_MSG) msgCount++;
}

// 设置默认状态
void setDefaultStatus(String line1, String line2, uint32_t color1 = 0x00FFAA, uint32_t color2 = 0xCCCCCC) {
defaultStatusLine1 = line1;
defaultStatusLine2 = line2;
defaultColor1 = color1;
defaultColor2 = color2;
if (!isTempStatus) {
drawUI(defaultStatusLine1, defaultStatusLine2, defaultColor1, defaultColor2);
}
}

// 显示临时状态
void showTempStatus(String line1, String line2, uint32_t color1, uint32_t color2) {
tempStatusLine1 = line1;
tempStatusLine2 = line2;
tempColor1 = color1;
tempColor2 = color2;
isTempStatus = true;
statusChangeTime = millis();
drawUI(tempStatusLine1, tempStatusLine2, tempColor1, tempColor2);
}

void checkRestoreStatus() {
if (isTempStatus && (millis() - statusChangeTime >= STATUS_DISPLAY_DURATION)) {
isTempStatus = false;
drawUI(defaultStatusLine1, defaultStatusLine2, defaultColor1, defaultColor2);
}
}

void updateStatus(String line1, String line2, uint32_t color1 = 0xFFFFFF, uint32_t color2 = 0x00FFAA) {
showTempStatus(line1, line2, color1, color2);
}

// ============= HTML 工具函数 =============
String escapeHtml(String s) {
s.replace("&", "&amp;");
s.replace("<", "&lt;");
s.replace(">", "&gt;");
s.replace("\"", "&quot;");
return s;
}

String escapeAttr(String s) {
s.replace("&", "&amp;");
s.replace("\"", "&quot;");
return s;
}

String getJsonValue(String data, String key) {
// 先尝试字符串格式："key":"value"
String stringKey = "\"" + key + "\":\"";
int start = data.indexOf(stringKey);
if (start != -1) {
start += stringKey.length();
int end = data.indexOf("\"", start);
if (end != -1) {
return data.substring(start, end);
}
}

    // 再尝试数值格式："key":123.45
    String numberKey = "\"" + key + "\":";
    start = data.indexOf(numberKey);
    if (start != -1) {
        start += numberKey.length();
        // 找到数值的结束位置（逗号、大括号或空格）
        int end = start;
        while (end < data.length()) {
            char c = data.charAt(end);
            if (c == ',' || c == '}' || c == ' ' || c == '\n' || c == '\r') {
                break;
            }
            end++;
        }
        if (end > start) {
            return data.substring(start, end);
        }
    }
    
    return "";
}

// ============= 配网 Web 页面 =============
void handleConfigPage() {
if (!scanDone) {
int n = WiFi.scanNetworks();
cachedSsidList = "";
for (int i = 0; i < n; ++i) {
String ssid = WiFi.SSID(i);
String safeSsid = escapeHtml(ssid);
String attrSsid = escapeAttr(ssid);
cachedSsidList += "<div class='item' data-ssid='" + attrSsid + "'>" +
safeSsid + " (" + WiFi.RSSI(i) + "dBm)</div>";
}
scanDone = true;
}

    String html = R"rawliteral(
    <!DOCTYPE html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
    <title>K10 配网</title>
    <style>
        body{font-family:sans-serif;padding:15px;background:#0A0A1A;color:#CCCCCC;}
        h2{color:#00FFAA;}
        input{width:100%;padding:10px;margin:10px 0;border-radius:5px;border:1px solid #334466;background:#1A1A2E;color:#FFF;box-sizing:border-box;}
        button{width:100%;padding:12px;background:#00FFAA;border:none;border-radius:5px;font-weight:bold;font-size:16px;color:#0A0A1A;}
        .item{background:#1A1A2E;padding:10px;margin:5px 0;border-radius:5px;cursor:pointer;border:1px solid #334466;}
        .item:hover{background:#2A2A4E;border-color:#00FFAA;}
    </style></head><body>
    <h2>📶 K10 WiFi 配置</h2>
    <form action="/save" method="POST">
        <input type="text" id="ssid" name="ssid" placeholder="WiFi名称" required>
        <input type="password" name="pass" placeholder="密码" required>
        <button type="submit">保存并重启</button>
    </form>
    <h3 style="color:#6688AA;">附近网络：</h3>
    <div id="list">)rawliteral" + cachedSsidList + R"rawliteral(</div>
    <script>
        document.getElementById('list').addEventListener('click', function(e) {
            var target = e.target.closest('.item');
            if (target) {
                document.getElementById('ssid').value = target.getAttribute('data-ssid');
            }
        });
    </script>
    </body></html>)rawliteral";
    server.send(200, "text/html", html);
}

void handleSaveWifi() {
if (server.hasArg("ssid") && server.hasArg("pass")) {
prefs.begin("wifi", false);
prefs.putString("ssid", server.arg("ssid"));
prefs.putString("pass", server.arg("pass"));
prefs.end();
server.send(200, "text/html", "<h1>✅ 已保存，设备重启中...</h1>");
delay(2000);
ESP.restart();
} else {
server.send(400, "text/html", "<h1>❌ 参数错误</h1>");
}
}

// ============= Minecraft 事件处理 =============
void handleEnvironmentData(String body) {
String playerName = getJsonValue(body, "player_name");
String requestId = getJsonValue(body, "request_id");
String temperature = getJsonValue(body, "temperature");
String humidity = getJsonValue(body, "humidity");
String light = getJsonValue(body, "light");
String windSpeed = getJsonValue(body, "wind_speed");
String weather = getJsonValue(body, "weather");
String biome = getJsonValue(body, "biome");

    // RGB灯闪烁提示
    k10.rgb->write(-1, 0x00AAFF);
    delay(200);
    k10.rgb->write(-1, 0x000000);
    delay(100);
    k10.rgb->write(-1, 0x00AAFF);
    delay(200);
    k10.rgb->write(-1, 0x000000);
    
    // 添加消息到聊天记录
    String envMsg = "[环境] 🌍 " + playerName;
    addMessage(envMsg);
    
    // 显示完整的环境数据UI（显示所有数值）
    drawEnvironmentUI(playerName, temperature, humidity, light, windSpeed, weather, biome);
    
    // 构建响应JSON
    String response = "{\"response_type\":\"acknowledgment\",\"status\":\"success\",\"request_id\":\"" + requestId + "\",\"message\":\"环境数据已接收\",\"data\":{\"temperature\":\"" + temperature + "\",\"humidity\":\"" + humidity + "\",\"light\":\"" + light + "\",\"wind_speed\":\"" + windSpeed + "\",\"weather\":\"" + weather + "\",\"biome\":\"" + biome + "\"}}";
    
    server.send(200, "application/json", response);
    
    // 记录到SD卡（包含所有数据）
    if (sdOk) {
        logFile = SD.open("/env_log.txt", FILE_APPEND);
        if (logFile) {
            logFile.println("[" + String(millis()) + "] " + playerName + " - T:" + temperature + "°C H:" + humidity + "% L:" + light + " W:" + windSpeed + "m/s Weather:" + weather + " Biome:" + biome);
            logFile.close();
        }
    }
    
    // 5秒后恢复默认UI
    delay(5000);
    drawUI(defaultStatusLine1, defaultStatusLine2, defaultColor1, defaultColor2);
}

void handleMcEvent() {
if (!server.hasArg("plain")) {
server.send(400, "application/json", "{\"status\":\"error\",\"msg\":\"missing body\"}");
return;
}

    String body = server.arg("plain");
    String event = getJsonValue(body, "event");
    String player = getJsonValue(body, "player");
    String msg = getJsonValue(body, "message");

    // 不再需要局部 lambda，直接使用全局 blinkLED

    if (event == "player_join") {
        blinkLED(0x00FF66);
        String logMsg = "[加入] ⭐ " + player;
        addMessage(logMsg);
        showTempStatus("✨ 欢迎!", player + " 加入了游戏", 0x00FF66, 0xFFFFFF);
    } 
    else if (event == "player_quit") {
        blinkLED(0xFFAA44);
        String logMsg = "[离开] 👋 " + player;
        addMessage(logMsg);
        showTempStatus("👋 再见!", player + " 离开了游戏", 0xFFAA44, 0xFFFFFF);
    }
    else if (event == "player_death") {
        blinkLED(0xFF3344);
        String logMsg = "[阵亡] 💀 " + player;
        addMessage(logMsg);
        showTempStatus("💀 啊哦!", player + " 阵亡了", 0xFF3344, 0xFFFFFF);
    }
    else if (event == "custom_msg") {
        blinkLED(0x44DDFF);
        String logMsg = "[消息] 💬 " + msg;
        addMessage(logMsg);
        showTempStatus("💬 来自MC:", msg, 0x44DDFF, 0xFFFFFF);
    }
    else if (event == "environment_data") {
        handleEnvironmentData(body);
        return;
    }
    else {
        server.send(200, "application/json", "{\"status\":\"ok\"}");
        return;
    }
    server.send(200, "application/json", "{\"status\":\"ok\"}");
}

// ============= 状态页 & 重置 =============
void handleStatusPage() {
String html = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><title>K10 Status</title>";
html += "<style>body{font-family:sans-serif;padding:15px;background:#0A0A1A;color:#CCCCCC;}";
html += "h1{color:#00FFAA;}a{color:#44DDFF;}</style></head><body>";
html += "<h1>🎮 K10 MC Bridge</h1>";
html += "<p>✅ 状态：<span style='color:#00FF66;'>运行中</span></p>";
html += "<p>📡 IP: <strong style='color:#44DDFF;'>" + deviceIP + "</strong></p>";
html += "<p>🔌 事件接口：<code style='background:#1A1A2E;padding:2px 8px;border-radius:3px;'>POST /mc_event</code></p>";
html += "<p><a href=\"/reset\" style='color:#FF6644;'>🔄 清除WiFi并重启</a></p>";
html += "</body></html>";
server.send(200, "text/html", html);
}

void handleReset() {
prefs.begin("wifi", false);
prefs.clear();
prefs.end();
server.send(200, "text/html", "<h1>🔄 已重置，设备重启中...</h1>");
delay(1500);
ESP.restart();
}

// ================= 主程序 =================
void setup() {
Serial.begin(115200);
k10.begin();
k10.initScreen(2);
k10.creatCanvas();

    if (SD.begin()) {
        sdOk = true;
        if (!SD.exists("/log.txt")) {
            File f = SD.open("/log.txt", FILE_WRITE);
            if (f) {
                f.println("=== K10 MC Bridge Log ===");
                f.close();
            }
        }
    } else {
        sdOk = false;
        Serial.println("SD卡初始化失败");
    }

    bool resetTriggered = false;
    if (k10.buttonA->isPressed()) {
        delay(50);
        if (k10.buttonA->isPressed()) {
            unsigned long t = millis();
            while (k10.buttonA->isPressed() && (millis() - t < 2000)) {
                delay(10);
            }
            if (k10.buttonA->isPressed()) {
                resetTriggered = true;
            }
        }
    }
    if (resetTriggered) {
        showTempStatus("🔄 重置WiFi...", "释放A键重启", 0xFF3344, 0xFFFFFF);
        prefs.begin("wifi", false);
        prefs.clear();
        prefs.end();
        delay(2000);
        ESP.restart();
    }

    prefs.begin("wifi", true);
    String saved_ssid = prefs.getString("ssid", "");
    String saved_pass = prefs.getString("pass", "");
    prefs.end();

    if (saved_ssid == "") {
        needApConfig = true;
    } else {
        WiFi.mode(WIFI_STA);
        WiFi.begin(saved_ssid.c_str(), saved_pass.c_str());
        int retry = 0;
        while (WiFi.status() != WL_CONNECTED && retry < 60) {
            showTempStatus("📶 连接中 " + String(retry+1) + "/60", saved_ssid, 0xFFFF44, 0xFFFFFF);
            delay(500);
            retry++;
        }
        if (WiFi.status() != WL_CONNECTED) {
            needApConfig = true;
        } else {
            deviceIP = WiFi.localIP().toString();
        }
    }

    if (needApConfig) {
        WiFi.mode(WIFI_AP);
        WiFi.softAP("K10-MC-Setup");
        setDefaultStatus("📶 配网模式", "连接热点 192.168.4.1", 0xFFAA44, 0xCCCCCC);
        server.on("/", handleConfigPage);
        server.on("/save", HTTP_POST, handleSaveWifi);
    } else {
        setDefaultStatus("✅ 已连接", "IP: " + deviceIP, 0x00FF66, 0xCCCCCC);
        addMessage("[系统] 🚀 设备已启动，IP: " + deviceIP);
        server.on("/", handleStatusPage);
        server.on("/mc_event", HTTP_POST, handleMcEvent);
        server.on("/reset", handleReset);
    }
    server.begin();
}

void loop() {
server.handleClient();
checkRestoreStatus();

    static unsigned long pressStart = 0;
    static bool resetDone = false;
    if (k10.buttonA->isPressed()) {
        if (pressStart == 0) {
            pressStart = millis();
        } else if (millis() - pressStart > 2000 && !resetDone) {
            resetDone = true;
            showTempStatus("🔄 重置WiFi...", "即将重启", 0xFF3344, 0xFFFFFF);
            prefs.begin("wifi", false);
            prefs.clear();
            prefs.end();
            delay(1000);
            ESP.restart();
        }
    } else {
        pressStart = 0;
        resetDone = false;
    }
    delay(2);
}