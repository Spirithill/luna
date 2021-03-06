package io.luna.game.model.item;

import io.luna.game.model.def.ItemDefinition;
import io.luna.game.model.mob.Player;
import io.luna.net.msg.out.GameChatboxMessageWriter;
import io.luna.net.msg.out.InventoryOverlayMessageWriter;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * An item container model representing a player's bank.
 *
 * @author lare96 <http://github.com/lare96>
 */
public final class Bank extends ItemContainer {

    /**
     * An adapter listening for bank changes.
     */
    private final class BankListener extends ItemContainerAdapter {

        /**
         * Creates a new {@link BankListener}.
         */
        public BankListener() {
            super(player);
        }

        @Override
        public int getWidgetId() {
            return BANK_DISPLAY_ID;
        }

        @Override
        public String getCapacityExceededMsg() {
            return "You do not have enough bank space to deposit that.";
        }
    }

    /**
     * The size.
     */
    public static final int SIZE = 352;

    /**
     * The main interface.
     */
    private static final int INTERFACE_ID = 5292;

    /**
     * The inventory overlay.
     */
    private static final int INVENTORY_OVERLAY_ID = 5063;

    /**
     * The bank item display.
     */
    public static final int BANK_DISPLAY_ID = 5382;

    /**
     * The inventory item display.
     */
    private static final int INVENTORY_DISPLAY_ID = 5064;

    /**
     * The withdraw mode state.
     */
    public static final int WITHDRAW_MODE_STATE_ID = 115;

    /**
     * The player.
     */
    private final Player player;

    /**
     * The inventory.
     */
    private final Inventory inventory;

    /**
     * Creates a new {@link Bank}.
     *
     * @param player The player.
     */
    public Bank(Player player) {
        super(SIZE, StackPolicy.ALWAYS);
        this.player = player;
        inventory = player.getInventory();

        addListener(new BankListener());
    }

    /**
     * Opens the banking interface.
     */
    public void open() {
        shift();

        player.queue(new InventoryOverlayMessageWriter(INTERFACE_ID, INVENTORY_OVERLAY_ID));
        player.setWithdrawAsNote(false);

        refresh();
    }

    /**
     * Deposits an item from the inventory. Returns {@code true} if successful.
     */
    public boolean deposit(int inventoryIndex, int amount) {
        Item inventoryItem = inventory.get(inventoryIndex);

        if (inventoryItem == null || amount < 1) {
            return false;
        }

        int existingAmount = inventory.computeAmountForId(inventoryItem.getId());
        if (amount > existingAmount) {
            amount = existingAmount;
        }
        inventoryItem = inventoryItem.createWithAmount(amount);

        ItemDefinition def = inventoryItem.getItemDef();
        Item depositItem = inventoryItem.createWithId(def.getUnnotedId().orElse(inventoryItem.getId()));

        int remaining = computeRemainingSize();
        Optional<Integer> depositIndex = computeIndexForId(depositItem.getId());
        if (remaining < 1 && !depositIndex.isPresent()) {
            fireCapacityExceededEvent();
            return false;
        }

        if (inventory.remove(inventoryItem)) {
            add(depositItem);
            refresh();
            return true;
        }
        return false;
    }

    /**
     * Withdraws an item from the bank. Returns {@code true} if successful.
     */
    public boolean withdraw(int bankIndex, int amount) {
        Item bankItem = get(bankIndex);

        if (bankItem == null || amount < 1) {
            return false;
        }

        int existingAmount = bankItem.getAmount();
        if (amount > existingAmount) {
            amount = existingAmount;
        }

        OptionalInt newId = OptionalInt.empty();
        if (player.isWithdrawAsNote()) {
            ItemDefinition def = bankItem.getItemDef();
            if (def.isNoteable()) {
                newId = def.getNotedId();
            } else {
                player.queue(new GameChatboxMessageWriter("This item cannot be withdrawn as a note."));
            }
        }
        Item withdrawItem = bankItem.createWithId(newId.orElse(bankItem.getId()));
        ItemDefinition newDef = withdrawItem.getItemDef();

        int remaining = inventory.computeRemainingSize();
        if (remaining < 1) {
            inventory.fireCapacityExceededEvent();
            return false;
        }

        if (amount > remaining && !newDef.isStackable()) {
            amount = remaining;
        }
        bankItem = bankItem.createWithAmount(amount);
        withdrawItem = withdrawItem.createWithAmount(amount);

        if (remove(bankItem)) {
            inventory.add(withdrawItem);
            refresh();
            return true;
        }
        return false;
    }

    /**
     * Refreshes the bank and inventory.
     */
    private void refresh() {
        player.queue(constructRefresh(BANK_DISPLAY_ID));
        player.queue(inventory.constructRefresh(INVENTORY_DISPLAY_ID));
    }
}
