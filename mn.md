/*!
* Minecraft × K10 联动接收器 (数字城市管理中心版)
* 新增完整城市管理系统UI，支持多界面切换
* 包含：聊天室、环境监控、城市仪表盘三大模块
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

// ==================== 数字城市管理系统 ====================

// ---------- 界面模式 ----------
enum DisplayMode {
  MODE_CHAT = 0,        // 聊天消息模式
  MODE_ENVIRONMENT,     // 环境监控模式
  MODE_CITY_DASHBOARD   // 城市仪表盘模式（新增）
};

DisplayMode currentDisplayMode = MODE_CHAT;
unsigned long lastModeChangeTime = 0;
const unsigned long AUTO_RETURN_TIMEOUT = 30000; // 30秒无操作返回聊天模式

// ---------- 城市仪表盘数据结构 ----------
struct CityBasicStats {
  int onlinePlayers;
  int maxPlayers;
  int playerLoad;
  float tps;
  String serverStatus;
  String cityStatus;
  float uptimeHours;
};

struct CityPopulationStats {
  int totalJoined;
  int currentOnline;
  int totalDeaths;
  int avgSessionTime;
  int peakToday;
};

struct CityEconomyStats {
  long totalTransactions;
  long totalVolume;
  int activeBankAccounts;
  double serverWealth;
};

struct CityActivityStats {
  long messagesSent;
  long blocksBroken;
  long blocksPlaced;
  String activityLevel;
};

// ---------- 城市数据缓存 ----------
CityBasicStats cityBasic = {0, 0, 0, 20.0, "NORMAL", "NORMAL", 0.0};
CityPopulationStats cityPopulation = {0, 0, 0, 0, 0};
CityEconomyStats cityEconomy = {0, 0, 0, 0.0};
CityActivityStats cityActivity = {0, 0, 0, "MINIMAL"};

// ---------- 最近事件列表 ----------
#define MAX_CITY_EVENTS 6
struct CityEventItem {
  String type;
  String source;
  String description;
  uint32_t color;
  unsigned long timestamp;
};

CityEventItem cityEvents[MAX_CITY_EVENTS];
int cityEventCount = 0;

// ---------- 城市初始化标志 ----------
bool cityInitialized = false;
String cityName = "Minecraft智慧城市";
unsigned long cityFoundedTime = 0;

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

// ============= 界面切换函数 =============
void switchToChatMode() {
currentDisplayMode = MODE_CHAT;
lastModeChangeTime = millis();
drawUI(defaultStatusLine1, defaultStatusLine2, defaultColor1, defaultColor2);
}

void switchToEnvironmentMode() {
currentDisplayMode = MODE_ENVIRONMENT;
lastModeChangeTime = millis();
}

void switchToCityDashboardMode() {
currentDisplayMode = MODE_CITY_DASHBOARD;
lastModeChangeTime = millis();
drawCityDashboard();
}

void cycleDisplayMode() {
switch (currentDisplayMode) {
case MODE_CHAT:
switchToEnvironmentMode();
break;
case MODE_ENVIRONMENT:
switchToCityDashboardMode();
break;
case MODE_CITY_DASHBOARD:
default:
switchToChatMode();
break;
}
}

void checkAutoReturnToChat() {
if (currentDisplayMode != MODE_CHAT && (millis() - lastModeChangeTime > AUTO_RETURN_TIMEOUT)) {
switchToChatMode();
}
}

// ---------- 屏幕工具 - 聊天UI ----------
void drawUI(String statusLine1, String statusLine2, uint32_t statusColor1 = 0x00FFAA, uint32_t statusColor2 = 0xCCCCCC) {
k10.canvas->canvasClear();
k10.setScreenBackground(0x0A0A1A);

    // 顶部状态区域
    k10.canvas->canvasText(statusLine1, 10, 10, statusColor1, k10.canvas->eCNAndENFont16, 50, true);
    k10.canvas->canvasText(statusLine2, 10, 38, statusColor2, k10.canvas->eCNAndENFont16, 45, true);

    // 模式指示器
    String modeIndicator = "[聊天]";
    k10.canvas->canvasText(modeIndicator, 180, 24, 0x6688AA, k10.canvas->eCNAndENFont16, 20, true);

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
            else if (display.startsWith("[城市]")) msgColor = 0xFFDD44;

            if (display.length() > 22) {
                display = display.substring(0, 22) + "...";
            }
            k10.canvas->canvasText(display, 12, y, msgColor, k10.canvas->eCNAndENFont16, 38, true);
            y += 26;
        }
    }

    // 底部装饰文字和操作提示
    k10.canvas->canvasText("◆ MC Bridge v3.0 ◆", 60, 288, 0x334466, k10.canvas->eCNAndENFont16, 25, true);
    k10.canvas->canvasText("按B键切换界面", 70, 268, 0x556677, k10.canvas->eCNAndENFont16, 18, true);

    k10.canvas->updateCanvas();
}

// ============= 环境数据专用显示UI =============
void drawEnvironmentUI(String playerName, String temperature, String humidity, String light, String windSpeed, String weather, String biome) {
k10.canvas->canvasClear();
k10.setScreenBackground(0x0A0A1A);

    // 标题区域
    k10.canvas->canvasText("🌍 环境数据监控", 10, 10, 0x00AAFF, k10.canvas->eCNAndENFont16, 50, true);
    k10.canvas->canvasText("玩家: " + playerName, 10, 35, 0xFFFFFF, k10.canvas->eCNAndENFont16, 45, true);

    // 模式指示器
    k10.canvas->canvasText("[环境]", 180, 24, 0x00AAFF, k10.canvas->eCNAndENFont16, 20, true);

    // 分隔线
    k10.canvas->canvasLine(10, 60, 220, 60, 0x334466);

    // 温度显示
    k10.canvas->canvasText("🌡️ 温度:", 15, 75, 0xFFAA44, k10.canvas->eCNAndENFont16, 30, true);
    k10.canvas->canvasText(temperature + "°C", 120, 75, 0xFFAA44, k10.canvas->eCNAndENFont16, 30, true);

    // 湿度显示
    k10.canvas->canvasText("💧 湿度:", 15, 100, 0x44DDFF, k10.canvas->eCNAndENFont16, 30, true);
    k10.canvas->canvasText(humidity + "%", 120, 100, 0x44DDFF, k10.canvas->eCNAndENFont16, 30, true);

    // 光照显示
    k10.canvas->canvasText("☀️ 光照:", 15, 125, 0xFFDD44, k10.canvas->eCNAndENFont16, 30, true);
    k10.canvas->canvasText(light, 120, 125, 0xFFDD44, k10.canvas->eCNAndENFont16, 30, true);

    // 风速显示
    k10.canvas->canvasText("💨 风速:", 15, 150, 0x66FF66, k10.canvas->eCNAndENFont16, 30, true);
    k10.canvas->canvasText(windSpeed + "m/s", 120, 150, 0x66FF66, k10.canvas->eCNAndENFont16, 30, true);

    // 天气显示
    k10.canvas->canvasText("🌤️ 天气:", 15, 175, 0xAA88FF, k10.canvas->eCNAndENFont16, 30, true);
    String weatherDisplay = weather;
    if (weather == "clear") weatherDisplay = "晴朗";
    else if (weather == "storm") weatherDisplay = "暴雨";
    else if (weather == "thunder") weatherDisplay = "雷暴";
    k10.canvas->canvasText(weatherDisplay, 120, 175, 0xAA88FF, k10.canvas->eCNAndENFont16, 30, true);

    // 生物群系显示
    k10.canvas->canvasText("🌿 群系:", 15, 200, 0x00FFAA, k10.canvas->eCNAndENFont16, 30, true);
    String biomeDisplay = biome;
    if (biomeDisplay.length() > 12) {
        biomeDisplay = biomeDisplay.substring(0, 12);
    }
    k10.canvas->canvasText(biomeDisplay, 120, 200, 0x00FFAA, k10.canvas->eCNAndENFont16, 30, true);

    // 底部装饰
    k10.canvas->canvasText("◆ 实时监控 ◆", 60, 288, 0x334466, k10.canvas->eCNAndENFont16, 25, true);
    k10.canvas->canvasText("按B键切换界面", 70, 268, 0x556677, k10.canvas->eCNAndENFont16, 18, true);

    k10.canvas->updateCanvas();
}

// ============= 🏙️ 城市管理中心专用显示UI =============
void drawCityDashboard() {
k10.canvas->canvasClear();
k10.setScreenBackground(0x0A0A1A);

    // ========== 标题区域 ==========
    k10.canvas->canvasText("🏙️ 数字城市中心", 10, 8, 0x00FFAA, k10.canvas->eCNAndENFont16, 48, true);
    k10.canvas->canvasText(cityName, 10, 32, 0xFFFFFF, k10.canvas->eCNAndENFont16, 40, true);

    // 模式指示器和运行时间
    k10.canvas->canvasText("[城市]", 175, 20, 0x00FFAA, k10.canvas->eCNAndENFont16, 20, true);

    // 分隔线
    k10.canvas->canvasLine(10, 58, 230, 58, 0x334466);

    // ========== 第一行：基础状态卡片 ==========
    // 在线人数卡片
    k10.canvas->canvasText("👥 在线", 12, 68, 0x44DDFF, k10.canvas->eCNAndENFont16, 22, true);
    k10.canvas->canvasText(String(cityBasic.onlinePlayers) + "/" + String(cityBasic.maxPlayers), 12, 90, 0xFFFFFF, k10.canvas->eCNAndENFont16, 24, true);

    // TPS状态卡片
    k10.canvas->canvasText("⚡ TPS", 85, 68, 0xFFDD44, k10.canvas->eCNAndENFont16, 22, true);
    String tpsStr = String(cityBasic.tps, 1);
    k10.canvas->canvasText(tpsStr, 85, 90, getTPSColor(cityBasic.tps), k10.canvas->eCNAndENFont16, 24, true);

    // 运行时间卡片
    k10.canvas->canvasText("⏱️ 运行", 155, 68, 0xAA88FF, k10.canvas->eCNAndENFont16, 22, true);
    String uptimeStr = formatUptime(cityBasic.uptimeHours);
    k10.canvas->canvasText(uptimeStr, 155, 90, 0xFFFFFF, k10.canvas->eCNAndENFont16, 24, true);

    // ========== 第二行：城市状态指示 ==========
    k10.canvas->canvasText("📊 城市状态:", 12, 118, 0xCCCCCC, k10.canvas->eCNAndENFont16, 24, true);

    // 状态颜色指示条
    uint32_t statusBarColor = getCityStatusColor(cityBasic.cityStatus);
    k10.canvas->canvasFillRect(110, 118, 100, 18, statusBarColor, statusBarColor);

    // 状态文字
    String statusText = translateCityStatus(cityBasic.cityStatus);
    k10.canvas->canvasText(statusText, 115, 120, 0x000000, k10.canvas->eCNAndENFont16, 22, true);

    // ========== 第三行：人口统计 ==========
    k10.canvas->canvasText("== 人口统计 ==", 12, 145, 0x6688AA, k10.canvas->eCNAndENFont16, 26, true);

    k10.canvas->canvasText("总入驻: " + String(cityPopulation.totalJoined), 15, 170, 0xCCCCCC, k10.canvas->eCNAndENFont16, 22, true);
    k10.canvas->canvasText("今日峰值: " + String(cityPopulation.peakToday), 120, 170, 0xFFDD44, k10.canvas->eCNAndENFont16, 22, true);

    k10.canvas->canvasText("总阵亡: " + String(cityPopulation.totalDeaths), 15, 195, 0xFF6666, k10.canvas->eCNAndENFont16, 22, true);
    k10.canvas->canvasText("均时长: " + String(cityPopulation.avgSessionTime) + "分", 120, 195, 0x66FF66, k10.canvas->eCNAndENFont16, 22, true);

    // ========== 第四行：活动统计 ==========
    k10.canvas->canvasText("== 活动统计 ==", 12, 220, 0x6688AA, k10.canvas->eCNAndENFont16, 26, true);

    String activityLevelText = translateActivityLevel(cityActivity.activityLevel);
    uint32_t activityColor = getActivityLevelColor(cityActivity.activityLevel);
    k10.canvas->canvasText("活跃度: " + activityLevelText, 15, 245, activityColor, k10.canvas->eCNAndENFont16, 22, true);

    // 底部信息
    k10.canvas->canvasText("◆ 智慧城市 v1.0 ◆", 55, 288, 0x334466, k10.canvas->eCNAndENFont16, 25, true);
    k10.canvas->canvasText("按B键切换界面", 70, 268, 0x556677, k10.canvas->eCNAndENFont16, 18, true);

    k10.canvas->updateCanvas();
}

// ============= 城市数据辅助函数 =============
uint32_t getTPSColor(float tps) {
if (tps >= 18.0) return 0x66FF66;
if (tps >= 15.0) return 0xFFDD44;
if (tps >= 10.0) return 0xFFAA44;
return 0xFF4444;
}

uint32_t getCityStatusColor(String status) {
if (status == "EXCELLENT") return 0x66FF66;
if (status == "NORMAL") return 0x44DDFF;
if (status == "BUSY") return 0xFFDD44;
if (status == "WARNING") return 0xFFAA44;
if (status == "CRITICAL") return 0xFF4444;
return 0xCCCCCC;
}

String translateCityStatus(String status) {
if (status == "EXCELLENT") return "卓越";
if (status == "NORMAL") return "正常";
if (status == "BUSY") return "繁忙";
if (status == "WARNING") return "警告";
if (status == "CRITICAL") return "紧急";
return status;
}

String translateActivityLevel(String level) {
if (level == "VERY_HIGH") return "极高";
if (level == "HIGH") return "高";
if (level == "MODERATE") return "中等";
if (level == "LOW") return "低";
if (level == "MINIMAL") return "极低";
return level;
}

uint32_t getActivityLevelColor(String level) {
if (level == "VERY_HIGH") return 0xFF4444;
if (level == "HIGH") return 0xFFAA44;
if (level == "MODERATE") return 0xFFDD44;
if (level == "LOW") return 0x44DDFF;
if (level == "MINIMAL") return 0x666666;
return 0xCCCCCC;
}

String formatUptime(float hours) {
if (hours < 1.0) {
int minutes = (int)(hours * 60);
return String(minutes) + "分";
} else if (hours < 24.0) {
return String((int)hours) + "时";
} else {
int days = (int)(hours / 24);
int remainderHours = (int)(hours) % 24;
return String(days) + "天" + String(remainderHours) + "时";
}
}

// ============= 城市事件管理 =============
void addCityEvent(String type, String source, String description, uint32_t color) {
// 移动现有事件
for (int i = MAX_CITY_EVENTS - 1; i > 0; i--) {
cityEvents[i] = cityEvents[i-1];
}

    // 添加新事件到头部
    cityEvents[0].type = type;
    cityEvents[0].source = source;
    cityEvents[0].description = description;
    cityEvents[0].color = color;
    cityEvents[0].timestamp = millis();

    if (cityEventCount < MAX_CITY_EVENTS) {
        cityEventCount++;
    }
}

// ============= 城市数据处理函数 =============
void processCityInitData(String body) {
cityName = getJsonValue(body, "city_name");
if (cityName == "") cityName = "Minecraft智慧城市";

String foundedStr = getJsonValue(body, "founded_date");
if (foundedStr != "") {
cityFoundedTime = foundedStr.toInt();
} else {
cityFoundedTime = millis();
}

cityInitialized = true;

addMessage("[城市] 🏙️ " + cityName + " 已初始化");
blinkLED(0x00FFAA);

Serial.println("[城市] 初始化完成: " + cityName);
}

void processCityDashboardData(String body) {
// 解析基础统计数据
String onlinePlayersStr = getJsonValue(body, "online_players");
String maxPlayersStr = getJsonValue(body, "max_players");
String tpsStr = getJsonValue(body, "tps");
String cityStatusStr = getJsonValue(body, "city_status");
String uptimeStr = getJsonValue(body, "uptime_hours");

if (onlinePlayersStr != "") cityBasic.onlinePlayers = onlinePlayersStr.toInt();
if (maxPlayersStr != "") cityBasic.maxPlayers = maxPlayersStr.toInt();
if (tpsStr != "") cityBasic.tps = tpsStr.toFloat();
if (cityStatusStr != "") cityBasic.cityStatus = cityStatusStr;
if (uptimeStr != "") cityBasic.uptimeHours = uptimeStr.toFloat();

cityBasic.playerLoad = cityBasic.maxPlayers > 0 ? (cityBasic.onlinePlayers * 100 / cityBasic.maxPlayers) : 0;

    // 解析人口统计
    String totalJoinedStr = getJsonValue(body, "total_joined");
    String totalDeathsStr = getJsonValue(body, "total_deaths");
    String avgSessionStr = getJsonValue(body, "avg_session_time");
    String peakTodayStr = getJsonValue(body, "peak_today");

    if (totalJoinedStr != "") cityPopulation.totalJoined = totalJoinedStr.toInt();
    if (totalDeathsStr != "") cityPopulation.totalDeaths = totalDeathsStr.toInt();
    if (avgSessionStr != "") cityPopulation.avgSessionTime = avgSessionStr.toInt();
    if (peakTodayStr != "") cityPopulation.peakToday = peakTodayStr.toInt();
    cityPopulation.currentOnline = cityBasic.onlinePlayers;

    // 解析活动统计
    String messagesStr = getJsonValue(body, "messages_sent");
    String blocksBrokenStr = getJsonValue(body, "blocks_broken");
    String blocksPlacedStr = getJsonValue(body, "blocks_placed");
    String activityLevelStr = getJsonValue(body, "activity_level");

    if (messagesStr != "") cityActivity.messagesSent = messagesStr.toInt();
    if (blocksBrokenStr != "") cityActivity.blocksBroken = blocksBrokenStr.toInt();
    if (blocksPlacedStr != "") cityActivity.blocksPlaced = blocksPlacedStr.toInt();
    if (activityLevelStr != "") cityActivity.activityLevel = activityLevelStr;

    // 如果当前在城市模式，刷新显示
    if (currentDisplayMode == MODE_CITY_DASHBOARD) {
        drawCityDashboard();
    }
}

void processCityEventData(String eventDataStr) {
String eventType = getJsonValue(eventDataStr, "type");
String eventSource = getJsonValue(eventDataStr, "source");
String eventDesc = getJsonValue(eventDataStr, "description");
String eventColorStr = getJsonValue(eventDataStr, "color");

    // 解析颜色（格式：#RRGGBB）
    uint32_t color = 0xFFFFFF;
    if (eventColorStr.startsWith("#")) {
        color = (uint32_t)strtol(eventColorStr.substring(1).c_str(), NULL, 16);
    }

    addCityEvent(eventType, eventSource, eventDesc, color);

    // 添加到聊天记录
    String chatMsg = "[城市] 🔔 " + eventDesc;
    addMessage(chatMsg);

    // 根据事件类型设置临时状态
    if (eventType == "PLAYER_JOIN") {
        showTempStatus("✨ 新市民", eventSource + " 加入城市", 0x66FF66, 0xFFFFFF);
    } else if (eventType == "PLAYER_QUIT") {
        showTempStatus("👋 市民离开", eventSource + " 离开城市", 0xFFAA44, 0xFFFFFF);
    } else if (eventType == "PLAYER_DEATH") {
        showTempStatus("💀 紧急事件", eventSource + " 阵亡", 0xFF4444, 0xFFFFFF);
        blinkLED(0xFF4444);
    } else if (eventType == "STATUS_CHANGE") {
        showTempStatus("⚠️ 状态变更", eventDesc, color, 0xFFFFFF);
        blinkLED(color);
    } else if (eventType == "MILESTONE") {
        showTempStatus("🎉 里程碑", eventDesc, 0xFFDD44, 0xFFFFFF);
        blinkLED(0xFFDD44);
    }
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
if (!isTempStatus && currentDisplayMode == MODE_CHAT) {
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
if (currentDisplayMode == MODE_CHAT) {
drawUI(tempStatusLine1, tempStatusLine2, tempColor1, tempColor2);
}
}

void checkRestoreStatus() {
if (isTempStatus && (millis() - statusChangeTime >= STATUS_DISPLAY_DURATION)) {
isTempStatus = false;
if (currentDisplayMode == MODE_CHAT) {
drawUI(defaultStatusLine1, defaultStatusLine2, defaultColor1, defaultColor2);
}
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

    // 尝试嵌套对象中的简单值
    // 处理 basic_stats.xxx 格式
    if (key.contains(".")) {
        String prefix = key.substring(0, key.indexOf('.'));
        String subkey = key.substring(key.indexOf('.') + 1);
        
        // 查找前缀对象
        String objKey = "\"" + prefix + "\":{";
        int objStart = data.indexOf(objKey);
        if (objStart != -1) {
            objStart += objKey.length() - 1; // 包含 {
            // 在对象内查找子键
            String subStringKey = "\"" + subkey + "\":\"";
            int subStart = data.indexOf(subStringKey, objStart);
            if (subStart != -1) {
                subStart += subStringKey.length();
                int subEnd = data.indexOf("\"", subStart);
                if (subEnd != -1) {
                    return data.substring(subStart, subEnd);
                }
            }
            
            // 尝试数值格式
            String subNumberKey = "\"" + subkey + "\":";
            subStart = data.indexOf(subNumberKey, objStart);
            if (subStart != -1) {
                subStart += subNumberKey.length();
                int subEnd = subStart;
                while (subEnd < data.length()) {
                    char c = data.charAt(subEnd);
                    if (c == ',' || c == '}' || c == ' ' || c == '\n') {
                        break;
                    }
                    subEnd++;
                }
                if (subEnd > subStart) {
                    return data.substring(subStart, subEnd);
                }
            }
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

    // 显示完整的环境数据UI
    drawEnvironmentUI(playerName, temperature, humidity, light, windSpeed, weather, biome);

    // 构建响应JSON
    String response = "{\"response_type\":\"acknowledgment\",\"status\":\"success\",\"request_id\":\"" + requestId + "\",\"message\":\"环境数据已接收\",\"data\":{\"temperature\":\"" + temperature + "\",\"humidity\":\"" + humidity + "\",\"light\":\"" + light + "\",\"wind_speed\":\"" + windSpeed + "\",\"weather\":\"" + weather + "\",\"biome\":\"" + biome + "\"}}";

    server.send(200, "application/json", response);

    // 记录到SD卡
    if (sdOk) {
        logFile = SD.open("/env_log.txt", FILE_APPEND);
        if (logFile) {
            logFile.println("[" + String(millis()) + "] " + playerName + " - T:" + temperature + "°C H:" + humidity + "% L:" + light + " W:" + windSpeed + "m/s Weather:" + weather + " Biome:" + biome);
            logFile.close();
        }
    }

    // 5秒后恢复默认UI或当前模式
    delay(5000);
    if (currentDisplayMode == MODE_CHAT) {
        drawUI(defaultStatusLine1, defaultStatusLine2, defaultColor1, defaultColor2);
    } else if (currentDisplayMode == MODE_CITY_DASHBOARD) {
        drawCityDashboard();
    }
}

void handleCityEvent(String body) {
String eventType = getJsonValue(body, "event_type");

if (eventType == "CITY_INIT") {
processCityInitData(body);
} else if (eventType == "CITY_DASHBOARD") {
processCityDashboardData(body);
} else if (eventType == "CITY_EVENT") {
// 提取嵌套的event对象
String eventData = extractJsonObject(body, "event");
if (eventData != "") {
processCityEventData(eventData);
}
} else if (eventType == "DETAILED_STATISTICS") {
// 详细统计数据处理（可选）
Serial.println("[城市] 收到详细统计数据");
}

server.send(200, "application/json", "{\"status\":\"ok\"}");
}

String extractJsonObject(String data, String key) {
String searchKey = "\"" + key + "\":{";
int start = data.indexOf(searchKey);
if (start == -1) return "";

start += searchKey.length() - 1; // 包含 {

int braceCount = 1;
int i = start;
while (i < data.length() && braceCount > 0) {
if (data.charAt(i) == '{') braceCount++;
else if (data.charAt(i) == '}') braceCount--;
i++;
}

if (braceCount == 0) {
return data.substring(start, i - 1);
}
return "";
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

    // 检查是否是城市数据事件
    String eventType = getJsonValue(body, "event_type");
    if (eventType.startsWith("CITY_") || eventType == "DETAILED_STATISTICS") {
        handleCityEvent(body);
        return;
    }

    // 传统事件处理
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
    server.send(200, "application/json", "{\"status\":\"ok\"");
}

// ============= 状态页 & 重置 =============
void handleStatusPage() {
String html = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><title>K10 Status</title>";
html += "<style>body{font-family:sans-serif;padding:15px;background:#0A0A1A;color:#CCCCCC;}";
html += "h1{color:#00FFAA;}a{color:#44DDFF;}</style></head><body>";
html += "<h1>🎮 K10 MC Bridge v3.0</h1>";
html += "<p>✅ 状态：<span style='color:#00FF66;'>运行中</span></p>";
html += "<p>📡 IP: <strong style='color:#44DDFF;'>" + deviceIP + "</strong></p>";
html += "<p>🏙️ 城市：<strong style='color:#00FFAA;'>" + (cityInitialized ? cityName : "未连接") + "</strong></p>";
html += "<p>🔌 事件接口：<code style='background:#1A1A2E;padding:2px 8px;border-radius:3px;'>POST /mc_event</code></p>";
html += "<hr style='border-color:#334466;'>";
html += "<h3 style='color:#6688AA;'>📊 城市实时数据：</h3>";
html += "<p>👥 在线: <strong>" + String(cityBasic.onlinePlayers) + "/" + String(cityBasic.maxPlayers) + "</strong></p>";
html += "<p>⚡ TPS: <strong style='color:#" + String(getTPSColor(cityBasic.tps), HEX) + ";'>" + String(cityBasic.tps, 1) + "</strong></p>";
html += "<p>📈 状态: <strong style='color:#" + String(getCityStatusColor(cityBasic.cityStatus), HEX) + ";'>" + translateCityStatus(cityBasic.cityStatus) + "</strong></p>";
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
                f.println("=== K10 MC Bridge Log v3.0 ===");
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
        addMessage("[系统] 🏙️ 数字城市系统就绪");
        server.on("/", handleStatusPage);
        server.on("/mc_event", HTTP_POST, handleMcEvent);
        server.on("/reset", handleReset);
    }
    server.begin();

    Serial.println("[系统] K10 MC Bridge v3.0 启动完成");
    Serial.println("[系统] 数字城市管理中心已激活");
}

void loop() {
server.handleClient();
checkRestoreStatus();
checkAutoReturnToChat();

    static unsigned long pressStart = 0;
    static bool resetDone = false;

    // A键长按重置WiFi
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

    // B键切换显示模式（短按）
    static bool lastBState = false;
    bool currentBState = k10.buttonB->isPressed();
    if (currentBState && !lastBState) {
        // B键刚被按下
        unsigned long pressTime = millis();
        while (k10.buttonB->isPressed() && (millis() - pressTime) < 500) {
            delay(10);
        }
        if (!k10.buttonB->isPressed() || (millis() - pressTime) >= 500) {
            // 短按或释放，执行切换
            cycleDisplayMode();
            blinkLED(0x00FFAA);
        }
    }
    lastBState = currentBState;

    delay(2);
}
