package cc.rayen.trevoraddons.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

public class TrevorAddonsClient implements ClientModInitializer {
    public static TrevorConfig CONFIG;

    @Override
    public void onInitializeClient() {
        CONFIG = TrevorConfig.load();
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> TrevorCommand.register(dispatcher));
        TrevorRuntime.init();
        TrevorEspRenderer.init();
    }
}
