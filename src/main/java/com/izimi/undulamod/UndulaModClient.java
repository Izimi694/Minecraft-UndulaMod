package com.izimi.undulamod;

import com.izimi.undulamod.client.StackRenderer;
import com.izimi.undulamod.client.UndulaParticleRenderer;
import com.izimi.undulamod.config.UndulaConfig;
import com.izimi.undulamod.network.UndulaParticlePacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UndulaModClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("undulamod-client");

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            UndulaParticleRenderer.tick();
            StackRenderer.tick();
        });

        ClientPlayNetworking.registerGlobalReceiver(UndulaParticlePacket.ID, (payload, context) -> {
            context.client().execute(() -> {

                if (payload.particleType() == 3) {
                    StackRenderer.update(payload.entityId(), (int) payload.radius());
                    return;
                }

                int life = switch (payload.particleType()) {
                    case 0 -> UndulaConfig.WAVE_LIFETIME;
                    case 1 -> UndulaConfig.TRANSFER_LIFETIME;
                    case 2 -> UndulaConfig.WHIRLPOOL_LIFETIME;
                    default -> 20;
                };
                UndulaParticleRenderer.add(payload.radius(), UndulaConfig.TILT_ANGLE_RAD, life,
                    payload.particleType(), payload.entityId(), payload.pos());
            });
        });
    }
}