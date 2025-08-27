package com.fabric.deepseekv3.muroj;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmoothSwitch implements ModInitializer {
	public static final String MOD_ID = "smoothswitch";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("SmoothSwitch 模组已加载！");

		// 注册服务器tick事件监听器
		ServerTickEvents.START_SERVER_TICK.register(server -> {
			// 在每个服务器tick处理玩家传送逻辑
			server.getPlayerManager().getPlayerList().forEach(VoidTeleportHandler::handleVoidTeleport);
		});
	}
}