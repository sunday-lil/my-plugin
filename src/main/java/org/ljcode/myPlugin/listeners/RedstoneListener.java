package org.ljcode.myPlugin.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.ljcode.myPlugin.managers.DigitalCityManager;

/**
 * 红石活动监听器（v1.2.0）
 *
 * 统计服务器红石线路的活动量并推送K10数字城市仪表盘。
 * 只统计"通断翻转"（0→N 或 N→0），忽略中间信号强度变化，
 * 避免比较器/时钟电路产生海量噪声事件；计数本身为原子操作，开销极小。
 */
public class RedstoneListener implements Listener {

    @EventHandler
    public void onBlockRedstoneChange(BlockRedstoneEvent event) {
        int oldCurrent = event.getOldCurrent();
        int newCurrent = event.getNewCurrent();

        // 只记录完全通断的翻转：亮→灭 或 灭→亮
        if ((oldCurrent == 0 && newCurrent > 0) || (oldCurrent > 0 && newCurrent == 0)) {
            DigitalCityManager cityManager = DigitalCityManager.getInstance();
            if (cityManager != null) {
                cityManager.recordRedstoneActivity();
            }
        }
    }
}
