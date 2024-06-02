package io.github.redvortexdev.skingrabber;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.include.com.google.common.base.Charsets;

import java.util.Base64;
import java.util.UUID;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class SkinGrabber implements ClientModInitializer {
    public static final String MOD_NAME = "Skin Grabber";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);
    public static MinecraftClient MC = MinecraftClient.getInstance();

    private static int grabCommand(String givenPlayerName, CommandContext<FabricClientCommandSource> context) {
        // Guard clauses to prevent weird behavior when not in a world
        if (context == null) return 0;
        if (MC.world == null) return 0;
        if (MC.player == null) return 0;

        ClientWorld world = MC.world;
        ClientPlayerEntity player = MC.player;

        PlayerEntity grabbedPlayer = null;

        // Attempt to get the PlayerEntity from the argument, if we fail resort to the nearest player
        if (givenPlayerName != null) {
            UUID givenPlayerUUID = MC.getSocialInteractionsManager().getUuid(givenPlayerName);
            if (givenPlayerUUID != Util.NIL_UUID) grabbedPlayer = world.getPlayerByUuid(givenPlayerUUID);
            if (isInvalid(grabbedPlayer)) {
                player.sendMessage(
                        Text.literal("")
                                .append(Text.literal("»")
                                        .formatted(Formatting.RED, Formatting.BOLD)
                                )
                                .append(Text.literal(" Could not find the specified player.")
                                )
                        );
                return 0;
            }
        }

        if (grabbedPlayer == null) {
            // Get the closest entity
            grabbedPlayer = world.getClosestPlayer(player.getX(), player.getY(), player.getZ(), 5D, (p) -> p != player);
            if (isInvalid(grabbedPlayer, player)) {
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
                                .append(Text.literal(" Make sure you are close to a player (6 blocks), alternatively run /grabskin <player>.")
                                        .formatted(Formatting.YELLOW)
                                )
                );
                return 0;
            }
        }


        // All the methods we are accessing should already be confirmed to not be null due to isInvalid()
        String skin = grabbedPlayer.getGameProfile().getProperties().get("textures").stream().findAny().get().value();

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

        LOGGER.info("Grabbing skin!\n-----------------------");
        LOGGER.info("Profile Name: " + profileName);
        LOGGER.info("Skin: " + skin);
        LOGGER.info("Decoded Skin: " + decodedSkin);
        LOGGER.info("URL: " + url + "\n-----------------------");

        player.sendMessage(Text.literal("")
                .append(Text.literal("»")
                        .formatted(Formatting.GREEN, Formatting.BOLD)
                )
                .append(Text.literal(" Successfully grabbed skin!")
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


    public static boolean isInvalid(PlayerEntity player) {
        return player == null || player.getGameProfile() == null || player.getGameProfile().getProperties() == null || player.getGameProfile().getProperties().get("textures").stream() == null || player.getGameProfile().getProperties().get("textures").stream().findAny().isEmpty();
    }

    public static boolean isInvalid(PlayerEntity player, PlayerEntity blacklistedPlayer) {
        return isInvalid(player) || player == blacklistedPlayer;
    }

    @Override
    public void onInitializeClient() {
        LOGGER.info("Hello Fabric world!");
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    literal("grabskin")
                            .executes(context -> grabCommand(null, context))
                            .then(
                                    argument("playerName", StringArgumentType.string())
                                            .executes(context -> grabCommand(StringArgumentType.getString(context, "playerName"), context))
                            )
            );
        });
    }
}