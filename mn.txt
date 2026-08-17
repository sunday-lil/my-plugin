/*!
Minecraft × K10 联动接收器 (数字城市管理中心版) - 优化版 v3.3
v3.3 更新（配合插件 v1.2.0）：
1. 界面精简为三页：城市大屏(默认) / 环境表格(融合页) / 玩家环境详情
   - 删除聊天模式与单玩家环境页（多余界面）
2. 加入/退出/阵亡/聊天等事件不再占用消息列表，直接全屏临时通知(~4秒)+LED反应
3. 环境表格：每玩家一行(名字+温度+湿度)，A/B键上下滚动选择，
   列表末尾固定"返回大屏"行，A+B同按确认：玩家行→详情页 / 返回行→大屏
4. 环境报告(每60秒)到达时只刷新表格数据+LED提示，不强制切页
5. 网络时序修复：配网/连接中页面在 networkReady 之前绝不被业务界面顶掉
6. 按键长按功能取消（A键专职上移），WiFi重置仅保留网页端 /reset 入口
v3.2 更新：
1. 默认界面改为城市大屏，30秒无操作自动返回大屏
2. 环境消息改为聚合列表（environment_summary，全玩家汇总，约60秒一条）
3. 城市仪表盘新增住户数（床+门=一户，插件端扫描）与红石活动显示
*/
#include "unihiker_k10.h"
#include <WiFi.h>
#include <WebServer.h>
#include <Preferences.h>
#include <SD.h>
#include <ArduinoJsonK10.h>      // 请安装 ArduinoJson 库 v6

// ================= 全局对象 =================
UNIHIKER_K10 k10;
WebServer server(80);
Preferences prefs;

bool needApConfig = false;
bool networkReady = false;        // v3.3: 网络就绪前不进入任何业务界面
String deviceIP = "";
String cachedSsidList = "";
bool scanDone = false;

String logBuffer = "";
bool sdOk = false;
unsigned long lastLogFlush = 0;
const unsigned long LOG_FLUSH_INTERVAL = 5000;

// ---------- 全屏临时通知层（事件直接反应，不占消息列表） ----------
String tempStatusLine1 = "";
String tempStatusLine2 = "";
uint32_t tempColor1 = 0xFFFFFF;
uint32_t tempColor2 = 0x00FFAA;
unsigned long statusChangeTime = 0;
bool isTempStatus = false;
const unsigned long STATUS_DISPLAY_DURATION = 4000;

// ==================== 数字城市管理系统 ====================

// ---------- 界面模式（v3.3: 三页） ----------
enum DisplayMode {
    MODE_CITY_DASHBOARD = 0,   // 默认主界面
    MODE_ENV_TABLE,            // 多玩家环境表格（融合页）
    MODE_ENV_DETAIL            // 单玩家环境详情（表格内A+B进入）
};

DisplayMode currentDisplayMode = MODE_CITY_DASHBOARD;
unsigned long lastModeChangeTime = 0;
const unsigned long AUTO_RETURN_TIMEOUT = 30000; // 30秒无操作返回城市大屏

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
    int households;   // 住户结构数（床+门=一户，插件端扫描）
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
    long redstoneChanges;  // 本周期红石通断翻转次数
};

// ---------- 城市数据缓存 ----------
CityBasicStats cityBasic = {0, 0, 0, 20.0, "NORMAL", "NORMAL", 0.0};
CityPopulationStats cityPopulation = {0, 0, 0, 0, 0, 0};
CityEconomyStats cityEconomy = {0, 0, 0, 0.0};
CityActivityStats cityActivity = {0, 0, 0, "MINIMAL", 0};

// ---------- 最近事件环形缓冲区（用于SD日志，不占屏幕） ----------
#define MAX_CITY_EVENTS 10
struct CityEventItem {
    String type;
    String source;
    String description;
    uint32_t color;
    unsigned long timestamp;
};

CityEventItem cityEvents[MAX_CITY_EVENTS];
int cityEventHead = 0;
int cityEventCount = 0;

// ---------- 城市初始化标志 ----------
bool cityInitialized = false;
String cityName = "Minecraft智慧城市";
unsigned long cityFoundedTime = 0;

// ---------- v3.3: 环境表格数据（与插件 max-players-in-report=20 对齐） ----------
#define MAX_ENV_ROWS 20          // 最多存储玩家行数
#define ENV_VISIBLE_ROWS 8       // 屏幕可视行数
struct EnvPlayerRow {
    String name;
    float temperature;
    float humidity;
    String light;
    String windSpeed;
    String weather;
    String biome;
};
EnvPlayerRow envRows[MAX_ENV_ROWS];
int envRowCount = 0;             // 已存储行数（≤MAX_ENV_ROWS）
int envPlayerTotal = 0;          // 插件报告的总玩家数
int envCursor = 0;               // 当前选中行（0..envRowCount，envRowCount为返回行）
int envScrollTop = 0;            // 滚动窗口起始行
int envDetailIndex = 0;          // 详情页对应的玩家行

// ---------- v3.3: 按钮状态（短按+组合键） ----------
bool aWasDown = false;
bool bWasDown = false;
unsigned long aDownTime = 0;
unsigned long bDownTime = 0;
bool comboFired = false;         // A+B组合键已触发标志（防止单键误触发）

// ============= 函数前置声明 =============
void drawCityDashboard();
void drawEnvTable();
void drawEnvDetail();
void drawNotification(String line1, String line2, uint32_t color1, uint32_t color2);
void drawConfigPortalUI();
void drawConnectingUI(int retry, String ssid);
String getJsonValue(String data, String key);
String escapeHtml(String s);
String escapeAttr(String s);
String extractJsonObject(String data, String key);
void appendLog(String msg);
void flushLogBuffer();
void showTempStatus(String line1, String line2, uint32_t color1, uint32_t color2);
void checkRestoreStatus();
void redrawCurrentMode();
void blinkLED(uint32_t color);
void switchToCityDashboardMode();
void switchToEnvTableMode();
void switchToEnvDetailMode(int rowIndex);
void checkAutoReturnToDashboard();
uint32_t getTPSColor(float tps);
uint32_t getCityStatusColor(String status);
String translateCityStatus(String status);
String translateActivityLevel(String level);
uint32_t getActivityLevelColor(String level);
String translateWeather(String weather);
String formatUptime(float hours);
String formatCompact(long n);
void addCityEvent(String type, String source, String description, uint32_t color);
void processCityInitData(String body);
void processCityDashboardData(String body);
void processCityEventData(String eventDataStr);
void handleEnvironmentSummary(String body);
void handleCityEvent(String body);
void handleMcEvent();
void handleConfigPage();
void handleSaveWifi();
void handleStatusPage();
void handleReset();
void handleButtons();
void onShortPressA();
void onShortPressB();
void onComboAB();
void envCursorUp();
void envCursorDown();

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
void switchToCityDashboardMode() {
    currentDisplayMode = MODE_CITY_DASHBOARD;
    lastModeChangeTime = millis();
    drawCityDashboard();
}

void switchToEnvTableMode() {
    currentDisplayMode = MODE_ENV_TABLE;
    lastModeChangeTime = millis();
    // 光标越界钳制（数据刷新后行数可能变少）
    if (envCursor > envRowCount) envCursor = envRowCount;
    drawEnvTable();
}

void switchToEnvDetailMode(int rowIndex) {
    if (rowIndex < 0 || rowIndex >= envRowCount) return;
    envDetailIndex = rowIndex;
    currentDisplayMode = MODE_ENV_DETAIL;
    lastModeChangeTime = millis();
    drawEnvDetail();
}

// 按当前模式重绘（通知超时/数据更新后恢复用）
void redrawCurrentMode() {
    switch (currentDisplayMode) {
        case MODE_ENV_TABLE:
            drawEnvTable();
            break;
        case MODE_ENV_DETAIL:
            drawEnvDetail();
            break;
        case MODE_CITY_DASHBOARD:
        default:
            drawCityDashboard();
            break;
    }
}

void checkAutoReturnToDashboard() {
    if (!networkReady) return;  // 配网/连接页面不受影响
    if (currentDisplayMode != MODE_CITY_DASHBOARD && (millis() - lastModeChangeTime > AUTO_RETURN_TIMEOUT)) {
        switchToCityDashboardMode();
    }
}

// ============= 🏙️ 城市仪表盘（默认主界面） =============
void drawCityDashboard() {
    k10.canvas->canvasClear();
    k10.setScreenBackground(0x0A0A1A);

    // ========== 标题区域 ==========
    k10.canvas->canvasText("🏙️ 数字城市中心", 10, 8, 0x00FFAA, k10.canvas->eCNAndENFont16, 48, true);
    k10.canvas->canvasText(cityName, 10, 32, 0xFFFFFF, k10.canvas->eCNAndENFont16, 40, true);

    k10.canvas->canvasText("[城市]", 175, 20, 0x00FFAA, k10.canvas->eCNAndENFont16, 20, true);

    // 分隔线
    k10.canvas->canvasLine(10, 58, 230, 58, 0x334466);

    // ========== 第一行：基础状态卡片 ==========
    k10.canvas->canvasText("👥 在线", 12, 68, 0x44DDFF, k10.canvas->eCNAndENFont16, 22, true);
    k10.canvas->canvasText(String(cityBasic.onlinePlayers) + "/" + String(cityBasic.maxPlayers), 12, 90, 0xFFFFFF, k10.canvas->eCNAndENFont16, 24, true);

    k10.canvas->canvasText("⚡ TPS", 85, 68, 0xFFDD44, k10.canvas->eCNAndENFont16, 22, true);
    String tpsStr = String(cityBasic.tps, 1);
    k10.canvas->canvasText(tpsStr, 85, 90, getTPSColor(cityBasic.tps), k10.canvas->eCNAndENFont16, 24, true);

    k10.canvas->canvasText("⏱️ 运行", 155, 68, 0xAA88FF, k10.canvas->eCNAndENFont16, 22, true);
    String uptimeStr = formatUptime(cityBasic.uptimeHours);
    k10.canvas->canvasText(uptimeStr, 155, 90, 0xFFFFFF, k10.canvas->eCNAndENFont16, 24, true);

    // ========== 第二行：城市状态指示 ==========
    k10.canvas->canvasText("📊 城市状态:", 12, 118, 0xCCCCCC, k10.canvas->eCNAndENFont16, 24, true);

    uint32_t statusBarColor = getCityStatusColor(cityBasic.cityStatus);
    k10.canvas->canvasRectangle(110, 118, 100, 18, statusBarColor, statusBarColor, true);

    String statusText = translateCityStatus(cityBasic.cityStatus);
    k10.canvas->canvasText(statusText, 115, 120, 0x000000, k10.canvas->eCNAndENFont16, 22, true);

    // ========== 第三行：人口统计 ==========
    k10.canvas->canvasText("== 人口统计 ==", 12, 145, 0x6688AA, k10.canvas->eCNAndENFont16, 26, true);

    k10.canvas->canvasText("总入驻: " + String(cityPopulation.totalJoined), 15, 170, 0xCCCCCC, k10.canvas->eCNAndENFont16, 22, true);
    k10.canvas->canvasText("今日峰值: " + String(cityPopulation.peakToday), 120, 170, 0xFFDD44, k10.canvas->eCNAndENFont16, 22, true);

    // 住户结构数（床+门=一户，插件端扫描）
    k10.canvas->canvasText("总阵亡: " + String(cityPopulation.totalDeaths), 15, 195, 0xFF6666, k10.canvas->eCNAndENFont16, 22, true);
    k10.canvas->canvasText("🏠 " + String(cityPopulation.households) + "户", 130, 195, 0x66FF66, k10.canvas->eCNAndENFont16, 22, true);

    // ========== 第四行：活动统计 ==========
    k10.canvas->canvasText("== 活动统计 ==", 12, 220, 0x6688AA, k10.canvas->eCNAndENFont16, 26, true);

    String activityLevelText = translateActivityLevel(cityActivity.activityLevel);
    uint32_t activityColor = getActivityLevelColor(cityActivity.activityLevel);
    k10.canvas->canvasText("活跃度: " + activityLevelText, 15, 245, activityColor, k10.canvas->eCNAndENFont16, 22, true);

    // 红石活动（本周期通断翻转次数，缩写显示）
    k10.canvas->canvasText("🔴RS " + formatCompact(cityActivity.redstoneChanges), 140, 245, 0xFF8866, k10.canvas->eCNAndENFont16, 24, true);

    // 底部信息
    k10.canvas->canvasText("◆ 智慧城市 v1.2 ◆", 55, 288, 0x334466, k10.canvas->eCNAndENFont16, 25, true);
    k10.canvas->canvasText("按B键 环境总览", 60, 268, 0x556677, k10.canvas->eCNAndENFont16, 20, true);

    k10.canvas->updateCanvas();
}

// ============= 🌍 多玩家环境表格（融合页：v3.3 核心界面） =============
// 每玩家一行: 名字+温度+湿度；A/B上下滚动选中；末尾"返回大屏"行；A+B确认
void drawEnvTable() {
    k10.canvas->canvasClear();
    k10.setScreenBackground(0x0A0A1A);

    // 标题区域
    k10.canvas->canvasText("🌍 环境总览", 10, 10, 0x00AAFF, k10.canvas->eCNAndENFont16, 48, true);
    k10.canvas->canvasText(String(envPlayerTotal) + "名玩家", 10, 34, 0xFFFFFF, k10.canvas->eCNAndENFont16, 40, true);

    // 滚动位置指示（当前选中/总行数，总行数含返回行）
    int totalItems = envRowCount + 1;
    k10.canvas->canvasText(String(envCursor + 1) + "/" + String(totalItems), 190, 34, 0x6688AA, k10.canvas->eCNAndENFont16, 18, true);

    // 分隔线
    k10.canvas->canvasLine(10, 58, 230, 58, 0x334466);

    // 表头
    k10.canvas->canvasText("名字", 14, 64, 0x6688AA, k10.canvas->eCNAndENFont16, 18, true);
    k10.canvas->canvasText("温度", 130, 64, 0x6688AA, k10.canvas->eCNAndENFont16, 18, true);
    k10.canvas->canvasText("湿度", 185, 64, 0x6688AA, k10.canvas->eCNAndENFont16, 18, true);

    if (envRowCount == 0) {
        // 尚未收到任何聚合报告
        k10.canvas->canvasText("等待插件环境报告...", 25, 130, 0x556677, k10.canvas->eCNAndENFont16, 35, true);
        k10.canvas->canvasText("约每60秒汇总一次", 30, 160, 0x334455, k10.canvas->eCNAndENFont16, 30, true);
    } else {
        // 数据行（滚动窗口）
        int y = 86;
        for (int screen = 0; screen < ENV_VISIBLE_ROWS; screen++) {
            int idx = envScrollTop + screen;
            if (idx > envRowCount) break;   // envRowCount 为返回行，含在内

            bool selected = (idx == envCursor);
            String name;
            String tempStr;
            String humStr;
            uint32_t nameColor, tempColor, humColor;

            if (idx == envRowCount) {
                // 末尾固定返回行
                name = "← 返回城市大屏";
                tempStr = "";
                humStr = "";
                nameColor = 0xFFAA44;
                tempColor = 0xFFAA44;
                humColor = 0xFFAA44;
            } else {
                name = envRows[idx].name;
                if (name.length() > 8) name = name.substring(0, 8);
                tempStr = String(envRows[idx].temperature, 1) + "°";
                humStr = String(envRows[idx].humidity, 0) + "%";
                nameColor = 0xCCCCCC;
                tempColor = 0xFFAA44;
                humColor = 0x44DDFF;
            }

            if (selected) {
                // 选中行: 高亮背景条 + 黑字
                k10.canvas->canvasRectangle(10, y - 3, 220, 22, 0x00AAFF, 0x00AAFF, true);
                k10.canvas->canvasText(name, 14, y, 0x000000, k10.canvas->eCNAndENFont16, 30, true);
                if (tempStr != "") {
                    k10.canvas->canvasText(tempStr, 130, y, 0x000000, k10.canvas->eCNAndENFont16, 26, true);
                    k10.canvas->canvasText(humStr, 185, y, 0x000000, k10.canvas->eCNAndENFont16, 22, true);
                }
            } else {
                k10.canvas->canvasText(name, 14, y, nameColor, k10.canvas->eCNAndENFont16, 30, true);
                if (tempStr != "") {
                    k10.canvas->canvasText(tempStr, 130, y, tempColor, k10.canvas->eCNAndENFont16, 26, true);
                    k10.canvas->canvasText(humStr, 185, y, humColor, k10.canvas->eCNAndENFont16, 22, true);
                }
            }
            y += 22;
        }

        // 滚动方向指示
        if (envScrollTop > 0) {
            k10.canvas->canvasText("▲", 224, 86, 0x556677, k10.canvas->eCNAndENFont16, 16, true);
        }
        if (envScrollTop + ENV_VISIBLE_ROWS <= envRowCount) {
            k10.canvas->canvasText("▼", 224, 244, 0x556677, k10.canvas->eCNAndENFont16, 16, true);
        }
    }

    // 底部提示
    k10.canvas->canvasText("A/B选择 A+B确认", 55, 268, 0x556677, k10.canvas->eCNAndENFont16, 20, true);
    k10.canvas->canvasText("◆ 60秒汇总 ◆", 65, 288, 0x334466, k10.canvas->eCNAndENFont16, 25, true);

    k10.canvas->updateCanvas();
}

// ============= 🔍 单玩家环境详情页（表格内A+B进入） =============
void drawEnvDetail() {
    if (envDetailIndex < 0 || envDetailIndex >= envRowCount) return;
    EnvPlayerRow* row = &envRows[envDetailIndex];

    k10.canvas->canvasClear();
    k10.setScreenBackground(0x0A0A1A);

    // 标题区域
    k10.canvas->canvasText("🌍 玩家环境详情", 10, 10, 0x00AAFF, k10.canvas->eCNAndENFont16, 48, true);
    k10.canvas->canvasText("玩家: " + row->name, 10, 35, 0xFFFFFF, k10.canvas->eCNAndENFont16, 45, true);

    // 分隔线
    k10.canvas->canvasLine(10, 60, 220, 60, 0x334466);

    // 温度
    k10.canvas->canvasText("🌡️ 温度:", 15, 75, 0xFFAA44, k10.canvas->eCNAndENFont16, 30, true);
    k10.canvas->canvasText(String(row->temperature, 1) + "°C", 120, 75, 0xFFAA44, k10.canvas->eCNAndENFont16, 30, true);

    // 湿度
    k10.canvas->canvasText("💧 湿度:", 15, 100, 0x44DDFF, k10.canvas->eCNAndENFont16, 30, true);
    k10.canvas->canvasText(String(row->humidity, 0) + "%", 120, 100, 0x44DDFF, k10.canvas->eCNAndENFont16, 30, true);

    // 光照
    k10.canvas->canvasText("☀️ 光照:", 15, 125, 0xFFDD44, k10.canvas->eCNAndENFont16, 30, true);
    k10.canvas->canvasText(row->light, 120, 125, 0xFFDD44, k10.canvas->eCNAndENFont16, 30, true);

    // 风速
    k10.canvas->canvasText("💨 风速:", 15, 150, 0x66FF66, k10.canvas->eCNAndENFont16, 30, true);
    k10.canvas->canvasText(row->windSpeed + "m/s", 120, 150, 0x66FF66, k10.canvas->eCNAndENFont16, 30, true);

    // 天气
    k10.canvas->canvasText("🌤️ 天气:", 15, 175, 0xAA88FF, k10.canvas->eCNAndENFont16, 30, true);
    k10.canvas->canvasText(translateWeather(row->weather), 120, 175, 0xAA88FF, k10.canvas->eCNAndENFont16, 30, true);

    // 生物群系
    k10.canvas->canvasText("🌿 群系:", 15, 200, 0x00FFAA, k10.canvas->eCNAndENFont16, 30, true);
    String biomeDisplay = row->biome;
    if (biomeDisplay.length() > 12) biomeDisplay = biomeDisplay.substring(0, 12);
    k10.canvas->canvasText(biomeDisplay, 120, 200, 0x00FFAA, k10.canvas->eCNAndENFont16, 30, true);

    // 底部提示
    k10.canvas->canvasText("按A或B返回列表", 55, 268, 0x556677, k10.canvas->eCNAndENFont16, 20, true);
    k10.canvas->canvasText("◆ 环境详情 ◆", 60, 288, 0x334466, k10.canvas->eCNAndENFont16, 25, true);

    k10.canvas->updateCanvas();
}

// ============= 全屏临时通知层（事件直接反应） =============
void drawNotification(String line1, String line2, uint32_t color1, uint32_t color2) {
    k10.canvas->canvasClear();
    k10.setScreenBackground(0x0A0A1A);

    k10.canvas->canvasText(line1, 10, 100, color1, k10.canvas->eCNAndENFont16, 50, true);
    k10.canvas->canvasText(line2, 10, 160, color2, k10.canvas->eCNAndENFont16, 45, true);

    // 上下装饰线
    k10.canvas->canvasLine(10, 85, 230, 85, 0x334466);
    k10.canvas->canvasLine(10, 225, 230, 225, 0x334466);

    k10.canvas->updateCanvas();
}

// ============= 配网模式专用页面 =============
void drawConfigPortalUI() {
    k10.canvas->canvasClear();
    k10.setScreenBackground(0x0A0A1A);

    k10.canvas->canvasText("📶 配网模式", 10, 80, 0xFFAA44, k10.canvas->eCNAndENFont16, 50, true);
    k10.canvas->canvasText("连接热点:", 10, 140, 0xFFFFFF, k10.canvas->eCNAndENFont16, 45, true);
    k10.canvas->canvasText("K10-MC-Setup", 10, 170, 0x00FFAA, k10.canvas->eCNAndENFont16, 45, true);
    k10.canvas->canvasText("浏览器打开:", 10, 210, 0xCCCCCC, k10.canvas->eCNAndENFont16, 40, true);
    k10.canvas->canvasText("192.168.4.1", 10, 240, 0x44DDFF, k10.canvas->eCNAndENFont16, 45, true);

    k10.canvas->updateCanvas();
}

// ============= WiFi连接中页面 =============
void drawConnectingUI(int retry, String ssid) {
    k10.canvas->canvasClear();
    k10.setScreenBackground(0x0A0A1A);

    k10.canvas->canvasText("📶 连接中 " + String(retry) + "/60", 10, 100, 0xFFFF44, k10.canvas->eCNAndENFont16, 45, true);
    k10.canvas->canvasText(ssid, 10, 160, 0xFFFFFF, k10.canvas->eCNAndENFont16, 45, true);
    k10.canvas->canvasText("请稍候...", 10, 210, 0x556677, k10.canvas->eCNAndENFont16, 40, true);

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

String translateWeather(String weather) {
    if (weather == "clear") return "晴朗";
    if (weather == "rain") return "下雨";
    if (weather == "storm") return "暴雨";
    if (weather == "thunder") return "雷暴";
    if (weather == "snow") return "下雪";
    return weather;
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

// 大数缩写显示（1234 → "1.2k"，5600000 → "5.6M"）
String formatCompact(long n) {
    if (n >= 1000000L) {
        return String(n / 1000000.0, 1) + "M";
    } else if (n >= 1000L) {
        return String(n / 1000.0, 1) + "k";
    }
    return String(n);
}

// ============= 城市事件环形缓冲区 =============
void addCityEvent(String type, String source, String description, uint32_t color) {
    cityEvents[cityEventHead] = {type, source, description, color, millis()};
    cityEventHead = (cityEventHead + 1) % MAX_CITY_EVENTS;
    if (cityEventCount < MAX_CITY_EVENTS) cityEventCount++;
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

    appendLog("[城市] " + cityName + " 已初始化");
    blinkLED(0x00FFAA);

    Serial.println("[城市] 初始化完成: " + cityName);
}

void processCityDashboardData(String body) {
    String onlinePlayersStr = getJsonValue(body, "basic_stats.online_players");
    String maxPlayersStr = getJsonValue(body, "basic_stats.max_players");
    String tpsStr = getJsonValue(body, "basic_stats.tps");
    String cityStatusStr = getJsonValue(body, "basic_stats.city_status");
    String uptimeStr = getJsonValue(body, "basic_stats.uptime_hours");

    if (onlinePlayersStr != "") cityBasic.onlinePlayers = onlinePlayersStr.toInt();
    if (maxPlayersStr != "") cityBasic.maxPlayers = maxPlayersStr.toInt();
    if (tpsStr != "") cityBasic.tps = tpsStr.toFloat();
    if (cityStatusStr != "") cityBasic.cityStatus = cityStatusStr;
    if (uptimeStr != "") cityBasic.uptimeHours = uptimeStr.toFloat();

    cityBasic.playerLoad = cityBasic.maxPlayers > 0 ? (cityBasic.onlinePlayers * 100 / cityBasic.maxPlayers) : 0;

    String totalJoinedStr = getJsonValue(body, "population_stats.total_joined");
    String totalDeathsStr = getJsonValue(body, "population_stats.total_deaths");
    String avgSessionStr = getJsonValue(body, "population_stats.avg_session_time");
    String peakTodayStr = getJsonValue(body, "population_stats.peak_today");
    String householdsStr = getJsonValue(body, "population_stats.households");

    if (totalJoinedStr != "") cityPopulation.totalJoined = totalJoinedStr.toInt();
    if (totalDeathsStr != "") cityPopulation.totalDeaths = totalDeathsStr.toInt();
    if (avgSessionStr != "") cityPopulation.avgSessionTime = avgSessionStr.toInt();
    if (peakTodayStr != "") cityPopulation.peakToday = peakTodayStr.toInt();
    if (householdsStr != "") cityPopulation.households = householdsStr.toInt();
    cityPopulation.currentOnline = cityBasic.onlinePlayers;

    String messagesStr = getJsonValue(body, "activity_stats.messages_sent");
    String blocksBrokenStr = getJsonValue(body, "activity_stats.blocks_broken");
    String blocksPlacedStr = getJsonValue(body, "activity_stats.blocks_placed");
    String activityLevelStr = getJsonValue(body, "activity_stats.activity_level");
    String redstoneStr = getJsonValue(body, "activity_stats.redstone_changes");

    if (messagesStr != "") cityActivity.messagesSent = messagesStr.toInt();
    if (blocksBrokenStr != "") cityActivity.blocksBroken = blocksBrokenStr.toInt();
    if (blocksPlacedStr != "") cityActivity.blocksPlaced = blocksPlacedStr.toInt();
    if (activityLevelStr != "") cityActivity.activityLevel = activityLevelStr;
    if (redstoneStr != "") cityActivity.redstoneChanges = redstoneStr.toInt();

    String totalTxStr = getJsonValue(body, "economy_stats.total_transactions");
    String totalVolStr = getJsonValue(body, "economy_stats.total_volume");
    String bankAcctStr = getJsonValue(body, "economy_stats.active_bank_accounts");
    String wealthStr = getJsonValue(body, "economy_stats.server_wealth");

    if (totalTxStr != "") cityEconomy.totalTransactions = totalTxStr.toInt();
    if (totalVolStr != "") cityEconomy.totalVolume = totalVolStr.toInt();
    if (bankAcctStr != "") cityEconomy.activeBankAccounts = bankAcctStr.toInt();
    if (wealthStr != "") cityEconomy.serverWealth = wealthStr.toDouble();

    Serial.println("[城市] 数据更新: 在线=" + String(cityBasic.onlinePlayers) +
                   ", TPS=" + String(cityBasic.tps) +
                   ", 总入驻=" + String(cityPopulation.totalJoined));

    // 数据变化时若正处于大屏则刷新（不强制切页）
    if (networkReady && !isTempStatus && currentDisplayMode == MODE_CITY_DASHBOARD) {
        drawCityDashboard();
    }
}

void processCityEventData(String eventDataStr) {
    String eventType = getJsonValue(eventDataStr, "type");
    String eventSource = getJsonValue(eventDataStr, "source");
    String eventDesc = getJsonValue(eventDataStr, "description");
    String eventColorStr = getJsonValue(eventDataStr, "color");

    uint32_t color = 0xFFFFFF;
    if (eventColorStr.startsWith("#")) {
        color = (uint32_t)strtol(eventColorStr.substring(1).c_str(), NULL, 16);
    }

    addCityEvent(eventType, eventSource, eventDesc, color);
    appendLog("[事件] " + eventType + " " + eventDesc);

    // v3.3: 事件不进消息列表，直接全屏通知+LED反应
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
    } else if (eventType == "HOUSING_CHANGE") {
        showTempStatus("🏠 住户变动", eventDesc, 0x66FF66, 0xFFFFFF);
        blinkLED(0x66FF66);
    } else if (eventType == "REDSTONE_SURGE") {
        showTempStatus("🔴 红石活动", eventDesc, 0xFF6644, 0xFFFFFF);
        blinkLED(0xFF6644);
    }
}

// ---------- 日志（v3.3: 仅SD卡，不进屏幕消息列表） ----------
void appendLog(String msg) {
    logBuffer += msg + "\n";
    if (logBuffer.length() > 1024) flushLogBuffer();
}

void flushLogBuffer() {
    if (!sdOk || logBuffer.length() == 0) return;
    File logFile = SD.open("/log.txt", FILE_APPEND);
    if (logFile) {
        logFile.print(logBuffer);
        logFile.close();
        logBuffer = "";
        lastLogFlush = millis();
    }
}

// ---------- 全屏临时通知管理 ----------
void showTempStatus(String line1, String line2, uint32_t color1, uint32_t color2) {
    tempStatusLine1 = line1;
    tempStatusLine2 = line2;
    tempColor1 = color1;
    tempColor2 = color2;
    isTempStatus = true;
    statusChangeTime = millis();
    drawNotification(tempStatusLine1, tempStatusLine2, tempColor1, tempColor2);
}

void checkRestoreStatus() {
    // v3.3 核心修复: 网络未就绪（配网/连接中）时绝不恢复业务界面，
    // 配网页面不会再被城市大屏顶掉
    if (!networkReady) return;

    if (isTempStatus && (millis() - statusChangeTime >= STATUS_DISPLAY_DURATION)) {
        isTempStatus = false;
        redrawCurrentMode();
    }
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

// ============= JSON 解析（使用 ArduinoJson） =============
String getJsonValue(String data, String key) {
    DynamicJsonDocument doc(2048);
    DeserializationError error = deserializeJson(doc, data);
    if (error) {
        return "";
    }

    if (key.indexOf('.') != -1) {
        String prefix = key.substring(0, key.indexOf('.'));
        String subkey = key.substring(key.indexOf('.') + 1);
        JsonVariant nested = doc[prefix][subkey];
        if (!nested.isNull()) {
            if (nested.is<String>()) return nested.as<String>();
            else return String(nested.as<float>());
        }
        return "";
    }

    JsonVariant value = doc[key];
    if (!value.isNull()) {
        if (value.is<String>()) return value.as<String>();
        else return String(value.as<float>());
    }
    return "";
}

// 提取 JSON 对象（用于 event 字段）
String extractJsonObject(String data, String key) {
    DynamicJsonDocument doc(2048);
    DeserializationError error = deserializeJson(doc, data);
    if (error) return "";
    JsonObject obj = doc[key];
    if (obj.isNull()) return "";
    String result;
    serializeJson(obj, result);
    return result;
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
            cachedSsidList += "<div class=\"item\" data-ssid=\"" + attrSsid + "\">" + safeSsid + " (" + WiFi.RSSI(i) + "dBm)</div>";
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
        server.send(200, "text/html", "<html><body><h2>✅ 已保存，设备重启中...</h2></body></html>");
        delay(2000);
        ESP.restart();
    } else {
        server.send(400, "text/html", "<html><body><h2>❌ 参数错误</h2></body></html>");
    }
}

// ============= 环境聚合报告（全玩家汇总列表，插件v1.2.0默认每60秒一条） =============
// v3.3: 只更新表格数据+LED，不强制切换界面；正在浏览表格页时原地刷新
void handleEnvironmentSummary(String body) {
    String requestId = getJsonValue(body, "request_id");
    String playerCountStr = getJsonValue(body, "player_count");

    // 使用 ArduinoJson 直接解析 players 数组（getJsonValue不支持数组）
    DynamicJsonDocument doc(12288);
    DeserializationError error = deserializeJson(doc, body);
    if (error) {
        server.send(200, "application/json", "{\"response_type\":\"acknowledgment\",\"status\":\"error\",\"request_id\":\"" + requestId + "\",\"message\":\"JSON解析失败\"}");
        return;
    }

    envRowCount = 0;
    envPlayerTotal = playerCountStr != "" ? playerCountStr.toInt() : 0;

    JsonArray players = doc["players"];
    if (!players.isNull()) {
        for (JsonObject playerObj : players) {
            if (envRowCount >= MAX_ENV_ROWS) break;
            envRows[envRowCount].name = playerObj["name"].as<String>();
            envRows[envRowCount].temperature = playerObj["temperature"].as<float>();
            envRows[envRowCount].humidity = playerObj["humidity"].as<float>();
            envRows[envRowCount].light = playerObj["light"].as<String>();
            envRows[envRowCount].windSpeed = playerObj["wind_speed"].as<String>();
            envRows[envRowCount].weather = playerObj["weather"].as<String>();
            envRows[envRowCount].biome = playerObj["biome"].as<String>();
            envRowCount++;
        }
        if (envPlayerTotal == 0) envPlayerTotal = players.size();
    }

    // 光标/滚动位置钳制（行数可能变少）
    if (envCursor > envRowCount) {
        envCursor = envRowCount;
        envScrollTop = envRowCount;
    }

    // LED 提示（环境报告到达）
    k10.rgb->write(-1, 0x00AAFF);
    delay(150);
    k10.rgb->write(-1, 0x000000);

    appendLog("[环境] 汇总: " + String(envPlayerTotal) + "名玩家");
    for (int i = 0; i < envRowCount; i++) {
        appendLog("  " + envRows[i].name + " T:" + String(envRows[i].temperature, 1) + "C H:" + String(envRows[i].humidity, 0) + "%");
    }

    // v3.3: 不强制切页；仅当用户正在浏览表格/详情页时原地刷新
    if (networkReady && !isTempStatus) {
        if (currentDisplayMode == MODE_ENV_TABLE) {
            drawEnvTable();
        } else if (currentDisplayMode == MODE_ENV_DETAIL) {
            drawEnvDetail();
        }
    }

    // 响应确认（带request_id，插件据此路由到环境调度器）
    String response = "{\"response_type\":\"acknowledgment\",\"status\":\"success\",\"request_id\":\"" + requestId + "\",\"message\":\"环境汇总已接收\",\"data\":{\"player_count\":" + String(envPlayerTotal) + ",\"displayed_rows\":" + String(envRowCount) + "}}";
    server.send(200, "application/json", response);
}

void handleCityEvent(String body) {
    String eventType = getJsonValue(body, "event_type");

    if (eventType == "CITY_INIT") {
        processCityInitData(body);
    } else if (eventType == "CITY_DASHBOARD") {
        processCityDashboardData(body);
    } else if (eventType == "CITY_EVENT") {
        String eventData = extractJsonObject(body, "event");
        if (eventData != "") {
            processCityEventData(eventData);
        }
    } else if (eventType == "DETAILED_STATISTICS") {
        Serial.println("[城市] 收到详细统计数据");
    }

    server.send(200, "application/json", "{\"status\":\"ok\"}");
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

    // v3.3: 传统事件不进消息列表，直接全屏通知+LED反应
    if (event == "player_join") {
        blinkLED(0x00FF66);
        showTempStatus("✨ 欢迎!", player + " 加入了游戏", 0x00FF66, 0xFFFFFF);
    } else if (event == "player_quit") {
        blinkLED(0xFFAA44);
        showTempStatus("👋 再见!", player + " 离开了游戏", 0xFFAA44, 0xFFFFFF);
    } else if (event == "player_death") {
        blinkLED(0xFF3344);
        showTempStatus("💀 啊哦!", player + " 阵亡了", 0xFF3344, 0xFFFFFF);
    } else if (event == "custom_msg") {
        blinkLED(0x44DDFF);
        showTempStatus("💬 来自MC:", msg, 0x44DDFF, 0xFFFFFF);
    } else if (event == "environment_summary") {
        // v3.3: 多玩家环境聚合报告
        handleEnvironmentSummary(body);
        return;
    } else if (event == "environment_data") {
        // v3.3: 单玩家环境报告已废弃（插件v1.2.0起只发聚合报告），静默确认
        appendLog("[环境] 收到已废弃的单玩家报告，忽略");
        server.send(200, "application/json", "{\"status\":\"ok\",\"deprecated\":true}");
        return;
    } else {
        server.send(200, "application/json", "{\"status\":\"ok\"}");
        return;
    }
    appendLog("[事件] " + event + " " + player + " " + msg);
    server.send(200, "application/json", "{\"status\":\"ok\"}");
}

// ============= 状态页 & 重置 =============
void handleStatusPage() {
    String html = R"rawliteral(
<!DOCTYPE html><meta charset="utf-8">
<html>
<body style="font-family:sans-serif;background:#0A0A1A;color:#CCCCCC;padding:20px;">
<h2 style="color:#00FFAA;">🎮 K10 MC Bridge v3.3</h2>
<p>✅ 状态：运行中</p>
<p>📡 IP: )rawliteral" + deviceIP + R"rawliteral(</p>
<p>🏙️ 城市：)rawliteral" + (cityInitialized ? cityName : "未连接") + R"rawliteral(</p>
<p>🔌 事件接口：POST /mc_event</p>
<hr>
<h3>📊 城市实时数据：</h3>
<p>👥 在线: )rawliteral" + String(cityBasic.onlinePlayers) + "/" + String(cityBasic.maxPlayers) + R"rawliteral(</p>
<p>⚡ TPS: )rawliteral" + String(cityBasic.tps, 1) + R"rawliteral(</p>
<p>📈 状态: )rawliteral" + translateCityStatus(cityBasic.cityStatus) + R"rawliteral(</p>
<p>🏠 住户: )rawliteral" + String(cityPopulation.households) + R"rawliteral(户（床+门结构）</p>
<p>🔴 红石活动: )rawliteral" + String(cityActivity.redstoneChanges) + R"rawliteral(次/周期</p>
<p>🌍 环境表格: )rawliteral" + String(envRowCount) + "/" + String(envPlayerTotal) + R"rawliteral( 玩家已收录</p>
<p>🏦 银行账户: )rawliteral" + String(cityEconomy.activeBankAccounts) + R"rawliteral( | 总交易: )rawliteral" + String(cityEconomy.totalTransactions) + R"rawliteral(</p>
<p><a href="/reset" style="color:#FF6644;">🔄 清除WiFi并重启</a></p>
</body></html>)rawliteral";

    server.send(200, "text/html", html);
}

void handleReset() {
    prefs.begin("wifi", false);
    prefs.clear();
    prefs.end();
    server.send(200, "text/html", "<html><body><h2>🔄 已重置，设备重启中...</h2></body></html>");
    delay(1500);
    ESP.restart();
}

// ============= v3.3: 按键处理（短按+组合键，无长按功能） =============
void handleButtons() {
    bool aNow = k10.buttonA->isPressed();
    bool bNow = k10.buttonB->isPressed();
    unsigned long now = millis();

    // 记录按下沿
    if (aNow && !aWasDown) {
        aDownTime = now;
        aWasDown = true;
    }
    if (bNow && !bWasDown) {
        bDownTime = now;
        bWasDown = true;
    }

    // 组合键检测：两键都按下且按下时刻接近（<400ms 视为同时）
    if (aNow && bNow && !comboFired) {
        long diff = (long)aDownTime - (long)bDownTime;
        if (diff < 0) diff = -diff;
        if (diff < 400) {
            comboFired = true;
            onComboAB();
        }
    }

    // 释放沿 → 短按（组合键已触发则消费掉，不误触发单键）
    if (!aNow && aWasDown) {
        aWasDown = false;
        if (!comboFired) onShortPressA();
    }
    if (!bNow && bWasDown) {
        bWasDown = false;
        if (!comboFired) onShortPressB();
    }
    // 两键全部松开后复位组合标志
    if (!aNow && !bNow) comboFired = false;
}

void onShortPressA() {
    if (!networkReady) return;
    lastModeChangeTime = millis();
    if (currentDisplayMode == MODE_ENV_TABLE) {
        envCursorUp();
    } else if (currentDisplayMode == MODE_ENV_DETAIL) {
        // 详情页按A返回列表
        switchToEnvTableMode();
    }
    // 大屏页A键无功能
}

void onShortPressB() {
    if (!networkReady) return;
    lastModeChangeTime = millis();
    if (currentDisplayMode == MODE_CITY_DASHBOARD) {
        // 大屏按B进入环境表格
        switchToEnvTableMode();
    } else if (currentDisplayMode == MODE_ENV_TABLE) {
        envCursorDown();
    } else if (currentDisplayMode == MODE_ENV_DETAIL) {
        // 详情页按B返回列表
        switchToEnvTableMode();
    }
}

void onComboAB() {
    if (!networkReady) return;
    lastModeChangeTime = millis();
    if (currentDisplayMode == MODE_ENV_TABLE) {
        if (envCursor >= envRowCount) {
            // 选中"返回大屏"行 → 回大屏
            switchToCityDashboardMode();
        } else {
            // 选中玩家行 → 进入详情页
            switchToEnvDetailMode(envCursor);
        }
    }
}

void envCursorUp() {
    if (envCursor > 0) envCursor--;
    // 滚动窗口跟随
    if (envCursor < envScrollTop) envScrollTop = envCursor;
    drawEnvTable();
}

void envCursorDown() {
    int maxCursor = envRowCount;  // 最后一项是返回行
    if (envCursor < maxCursor) envCursor++;
    if (envCursor > envScrollTop + ENV_VISIBLE_ROWS - 1) {
        envScrollTop = envCursor - ENV_VISIBLE_ROWS + 1;
    }
    drawEnvTable();
}

// ================= 主程序 =================
void setup() {
    Serial.begin(115200);
    k10.begin();
    k10.initScreen(2);
    k10.creatCanvas();

    // SD 卡初始化
    if (SD.begin()) {
        sdOk = true;
        if (!SD.exists("/log.txt")) {
            File f = SD.open("/log.txt", FILE_WRITE);
            if (f) {
                f.println("=== K10 MC Bridge Log v3.3 ===");
                f.close();
            }
        }
    } else {
        sdOk = false;
        Serial.println("SD卡初始化失败");
    }

    // v3.3: 网络就绪前只显示配网/连接页面，任何业务界面（大屏等）不会提前出现
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
            drawConnectingUI(retry + 1, saved_ssid);
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
        drawConfigPortalUI();
        appendLog("[系统] 进入配网模式");
        server.on("/", handleConfigPage);
        server.on("/save", HTTP_POST, handleSaveWifi);
    } else {
        // v3.3: 连接成功才置 networkReady，此后才允许进入城市大屏
        networkReady = true;
        appendLog("[系统] 设备已启动 IP: " + deviceIP);
        server.on("/", handleStatusPage);
        server.on("/mc_event", HTTP_POST, handleMcEvent);
        server.on("/reset", handleReset);

        switchToCityDashboardMode();
    }
    server.begin();

    Serial.println("[系统] K10 MC Bridge v3.3 启动完成");
}

void loop() {
    server.handleClient();
    if (networkReady) {
        checkRestoreStatus();
        checkAutoReturnToDashboard();
    }
    handleButtons();
    flushLogBuffer();

    delay(10);
}
