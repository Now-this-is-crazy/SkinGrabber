package io.github.redvortexdev.skingrabber;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.authlib.properties.Property;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.include.com.google.common.base.Charsets;

import java.net.URI;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class SkinGrabber implements ClientModInitializer {

    public static final String MOD_NAME = "Skin Grabber";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);
    public static MinecraftClient MC = MinecraftClient.getInstance();

    private static final TextColor SUCCESS = TextColor.fromRgb(0x7FFF7F);
    private static final TextColor SUCCESS_HIGHLIGHT = TextColor.fromRgb(0xAAFFAA);
    private static final TextColor SUCCESS_TEXT = TextColor.fromRgb(0xFFFFFF);
    private static final TextColor DARK_PREFIX = TextColor.fromRgb(0xAAAAAA);
    private static final TextColor NOTE = TextColor.fromRgb(0xFFD47F);
    private static final TextColor NOTE_TEXT = TextColor.fromRgb(0xFFFFAA);
    private static final TextColor ERROR = TextColor.fromRgb(0xFF5555);

    private static void grabCommand(String givenPlayerName, CommandContext<FabricClientCommandSource> context) {
        if (context == null) return;
        if (MC.world == null) return;
        if (MC.player == null) return;

        ClientWorld world = MC.world;
        ClientPlayerEntity player = MC.player;

        PlayerEntity grabbedPlayer = null;

        // Attempt to get the PlayerEntity from the argument, if we fail, resort to the nearest player
        if (givenPlayerName != null) {
            UUID givenPlayerUUID = MC.getSocialInteractionsManager().getUuid(givenPlayerName);
            if (givenPlayerUUID != Util.NIL_UUID) grabbedPlayer = world.getPlayerByUuid(givenPlayerUUID);
            if (grabbedPlayer == null || isInvalid(grabbedPlayer)) {
                player.sendMessage(
                        Text.literal("")
                                .append(Text.literal("»")
                                        .setStyle(Style.EMPTY.withColor(ERROR).withBold(true))
                                )
                                .append(Text.literal(" Could not find the specified player.")
                                )
                        , false);
                return;
            }
        }

        if (grabbedPlayer == null) {
            // Get the closest entity
            grabbedPlayer = world.getClosestPlayer(player.getX(), player.getY(), player.getZ(), 5D, (p) -> p != player);
            if (grabbedPlayer == null || isInvalid(grabbedPlayer, player)) {
                player.sendMessage(
                        Text.literal("")
                                .append(Text.literal("»")
                                        .setStyle(Style.EMPTY.withColor(ERROR).withBold(true))
                                )
                                .append(Text.literal(" Could not find a valid player.")
                                )
                                .append(Text.literal("\nℹ")
                                        .setStyle(Style.EMPTY.withColor(NOTE))
                                )
                                .append(Text.literal(" Make sure you are close to a player (6 blocks), alternatively run /grabskin <player>.")
                                        .setStyle(Style.EMPTY.withColor(NOTE_TEXT))
                                )
                        , false);
                return;
            }
        }


        // All the methods we are accessing should already be confirmed to not be null due to isInvalid()
        Optional<Property> optionalSkin = grabbedPlayer.getGameProfile().getProperties().get("textures").stream().findFirst();
        if (optionalSkin.isEmpty()) {
            // isInvalid already checks if the list is empty, but this is to get rid of the warning
            return;
        }
        String skin = optionalSkin.get().value();

        byte[] byteArray;
        try {
            byteArray = Base64.getDecoder().decode(skin);
        } catch (IllegalArgumentException ignored) {
            return;
        }
        String decodedSkin = new String(byteArray, Charsets.UTF_8);

        JsonElement jsonElement = JsonParser.parseString(decodedSkin);
        String profileName = jsonElement.getAsJsonObject().get("profileName").getAsString();
        String url = jsonElement.getAsJsonObject().getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();

        LOGGER.info("Grabbing skin!\n-----------------------");
        LOGGER.info("Profile Name: {}", profileName);
        LOGGER.info("Skin: {}", skin);
        LOGGER.info("Decoded Skin: {}", decodedSkin);
        LOGGER.info("URL: {}\n-----------------------", url);

        player.sendMessage(Text.literal("")
                        .append(Text.literal("»")
                                .setStyle(Style.EMPTY.withColor(SUCCESS).withBold(true))
                        )
                        .append(Text.literal(" Successfully grabbed skin!")
                        )
                        .append(Text.literal("\n›")
                                .setStyle(Style.EMPTY.withColor(DARK_PREFIX))
                        )
                        .append(Text.literal(" Profile Name: ")
                                .setStyle(Style.EMPTY.withColor(SUCCESS_HIGHLIGHT))
                        )
                        .append(Text.literal(profileName)
                                .setStyle(Style.EMPTY.withColor(SUCCESS_TEXT))
                        )
                        .append(Text.literal("\n›")
                                .setStyle(Style.EMPTY.withColor(DARK_PREFIX))
                        )
                        .append(Text.literal(" URL: ")
                                .setStyle(Style.EMPTY.withColor(SUCCESS_HIGHLIGHT))
                        )
                        .append(Text.literal(url)
                                .setStyle(Style.EMPTY
                                        .withColor(SUCCESS_TEXT)
                                        .withClickEvent(new ClickEvent.OpenUrl(URI.create(url)))
                                        .withHoverEvent(new HoverEvent.ShowText(
                                                Text.literal("Click to open.")
                                                        .styled(s2 -> s2.withColor(Formatting.GRAY))
                                        ))
                                )
                        )
                        .append(Text.literal("\nℹ")
                                .setStyle(Style.EMPTY.withColor(NOTE))
                        )
                        .append(Text.literal(" Check the Console for more information.")
                                .setStyle(Style.EMPTY.withColor(NOTE_TEXT))
                        )
                , false);

        player.playSound(SoundEvents.BLOCK_COPPER_BULB_TURN_ON, 1.0F, 1.0F);

    }


    public static boolean isInvalid(PlayerEntity player) {
        return player.getGameProfile() == null || player.getGameProfile().getProperties() == null || player.getGameProfile().getProperties().get("textures").isEmpty();
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
                            .executes(context -> {
                                grabCommand(null, context);
                                return 0;
                            })
                            .then(
                                    argument("playerName", StringArgumentType.string())
                                            .executes(context -> {
                                                grabCommand(StringArgumentType.getString(context, "playerName"), context);
                                                return 0;
                                            })
                            )
            );
        });
    }

}
