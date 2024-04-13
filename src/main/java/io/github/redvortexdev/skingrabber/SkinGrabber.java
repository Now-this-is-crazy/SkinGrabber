package io.github.redvortexdev.skingrabber;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.include.com.google.common.base.Charsets;

import java.util.Base64;

public class SkinGrabber implements ClientModInitializer {
    public static final String MOD_ID = "skingrabber";
    public static final String MOD_NAME = "Skin Grabber";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);
    public static MinecraftClient MC = MinecraftClient.getInstance();

    private static void onCommandRegistration(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(ClientCommandManager.literal("grabskin").executes(SkinGrabber::onCommandSent));
    }

    private static int onCommandSent(CommandContext<FabricClientCommandSource> context) {
        if (context == null) return 0;
        if (MC.world == null) return 0;
        if (MC.player == null) return 0;

        ClientWorld world = MC.world;
        ClientPlayerEntity player = MC.player;

        PlayerEntity closestPlayer = world.getClosestPlayer(player.getX(), player.getY(), player.getZ(), 4D, (p) -> p != player);
        if (closestPlayer == null || closestPlayer == player || closestPlayer.getGameProfile() == null || closestPlayer.getGameProfile().getProperties() == null || closestPlayer.getGameProfile().getProperties().get("textures").stream() == null || closestPlayer.getGameProfile().getProperties().get("textures").stream().findAny().isEmpty())
        {
            player.sendMessage(
                    Text.literal("")
                            .append(Text.literal("»")
                                    .formatted(Formatting.RED, Formatting.BOLD)
                            )
                            .append(Text.literal(" Could not find a valid player.")
                            )
                            .append(Text.literal("\nℹ")
                                    .formatted(Formatting.GOLD)
                            )
                            .append(Text.literal(" Make sure you are close to a player (5 blocks).")
                                    .formatted(Formatting.YELLOW)
                            )
            );
            return 0;
        }

        LOGGER.info("Grabbing skin!\n-----------------------");

        String skin = closestPlayer.getGameProfile().getProperties().get("textures").stream().findAny().get().value();

        byte[] byteArray;
        try {
            byteArray = Base64.getDecoder().decode(skin);
        } catch (IllegalArgumentException ignored) {
            return 0;
        }
        String decodedSkin = new String(byteArray, Charsets.UTF_8);

        JsonElement jsonElement = JsonParser.parseString(decodedSkin);
        String profileName = jsonElement.getAsJsonObject().get("profileName").getAsString();
        String url = jsonElement.getAsJsonObject().getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();

        LOGGER.info("Profile Name: " + profileName);
        LOGGER.info("Skin: " + skin);
        LOGGER.info("Decoded Skin: " + decodedSkin);
        LOGGER.info("URL: " + url + "\n-----------------------");

        player.sendMessage(Text.literal("")
                .append(Text.literal("»")
                        .formatted(Formatting.GREEN, Formatting.BOLD)
                )
                .append(Text.literal(" Succesfully grabbed skin!")
                )
                .append(Text.literal("\n›")
                        .formatted(Formatting.DARK_GRAY)
                )
                .append(Text.literal(" Profile Name: ")
                        .formatted(Formatting.GRAY)
                )
                .append(Text.literal(profileName)
                        .formatted(Formatting.WHITE)
                )
                .append(Text.literal("\n›")
                        .formatted(Formatting.DARK_GRAY)
                )
                .append(Text.literal(" URL: ")
                        .formatted(Formatting.GRAY)
                )
                .append(Text.literal(url)
                        .formatted(Formatting.WHITE)
                        .setStyle(Style.EMPTY
                                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        Text.literal("Click to open.")
                                                .formatted(Formatting.DARK_GRAY)
                                ))
                        )
                )
                .append(Text.literal("\nℹ")
                        .formatted(Formatting.GOLD)
                )
                .append(Text.literal(" Check the Console for more information.")
                        .formatted(Formatting.YELLOW)
                )
        );

        player.playSound(SoundEvent.of(new Identifier("minecraft:player.levelup")), 1.0F, 1.0F);

        return 0;
    }

    @Override
    public void onInitializeClient() {
        LOGGER.info("Hello Fabric world!");
        ClientCommandRegistrationCallback.EVENT.register(SkinGrabber::onCommandRegistration);
    }
}