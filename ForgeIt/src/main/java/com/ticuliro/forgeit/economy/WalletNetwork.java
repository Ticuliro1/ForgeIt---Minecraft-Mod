package com.ticuliro.forgeit.economy;

import com.ticuliro.forgeit.ForgeIt;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import java.util.Map;
import java.util.WeakHashMap;

public final class WalletNetwork {
    private static final Map<ServerPlayer, Long> LAST_ACTION = new WeakHashMap<>();
    private WalletNetwork() {}
    public record Balance(long amount) implements CustomPacketPayload {
        public static final Type<Balance> TYPE = new Type<>(ForgeIt.id("wallet_balance"));
        public static final StreamCodec<ByteBuf, Balance> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_LONG, Balance::amount, Balance::new);
        @Override public Type<Balance> type() { return TYPE; }
    }
    public record Action(int action, int denomination, int count) implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(ForgeIt.id("wallet_action"));
        public static final StreamCodec<ByteBuf, Action> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Action::action, ByteBufCodecs.VAR_INT, Action::denomination,
            ByteBufCodecs.VAR_INT, Action::count, Action::new);
        @Override public Type<Action> type() { return TYPE; }
    }
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("2");
        registrar.playToClient(Balance.TYPE, Balance.CODEC, (payload, ctx) -> {
            if (payload.amount >= 0 && payload.amount <= Wallet.MAX_BALANCE) ctx.player().setData(ForgeIt.WALLET, payload.amount);
        });
        registrar.playToServer(Action.TYPE, Action.CODEC, WalletNetwork::handle);
    }
    public static void sync(ServerPlayer player) {
        // Fake players used by tests/automation have no negotiated client payload channels.
        if (player.connection != null && net.neoforged.neoforge.network.registration.NetworkRegistry.hasChannel(player.connection, Balance.TYPE.id()))
            PacketDistributor.sendToPlayer(player, new Balance(Wallet.balance(player)));
    }
    public static void handle(Action action, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) perform(player, action);
    }
    public static boolean perform(ServerPlayer player, Action action) {
        if (!player.isAlive() || player.isSpectator() || player.containerMenu != player.inventoryMenu) return false;
        if (action.action < 0 || action.action > 2) return false;
        long now = player.serverLevel().getGameTime(); Long last = LAST_ACTION.get(player);
        if (last != null && now - last < 5) return false;
        LAST_ACTION.put(player, now);
        if (action.action == 2) { sync(player); return true; }
        Wallet.Result result = action.action == 0 ? Wallet.withdraw(player, Coin.byIndex(action.denomination), action.count) : Wallet.depositInventory(player, false);
        player.displayClientMessage(Component.translatable("message.forgeit.wallet." + result.name().toLowerCase(java.util.Locale.ROOT)), true);
        sync(player); return result == Wallet.Result.SUCCESS;
    }
}
