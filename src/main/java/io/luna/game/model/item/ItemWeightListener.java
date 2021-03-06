package io.luna.game.model.item;

import io.luna.game.model.def.ItemDefinition;
import io.luna.game.model.mob.Player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * A listener that will update a player's weight.
 *
 * @author lare96 <http://github.org/lare96>
 */
public final class ItemWeightListener implements ItemContainerListener {

    /**
     * The player.
     */
    private final Player player;

    /**
     * A list of weight changes.
     */
    private final List<Double> weightChanges = new ArrayList<>();

    /**
     * Creates a new {@link ItemWeightListener}.
     *
     * @param player The player.
     */
    public ItemWeightListener(Player player) {
        this.player = player;
    }

    @Override
    public void onSingleUpdate(ItemContainer items, Optional<Item> oldItem, Optional<Item> newItem, int index) {
        updateWeight(oldItem, newItem);
    }

    @Override
    public void onBulkUpdate(ItemContainer items, Optional<Item> oldItem, Optional<Item> newItem, int index) {
        weightChanges.add(computeWeightDifference(oldItem, newItem));
    }

    @Override
    public void onBulkUpdateCompleted(ItemContainer items) {
        Iterator<Double> iterator = weightChanges.iterator();
        double currentWeight = player.getWeight();

        while (iterator.hasNext()) {
            currentWeight += iterator.next();
            iterator.remove();
        }
        player.setWeight(currentWeight);
    }

    /**
     * Updates the weight for a single item set.
     */
    private void updateWeight(Optional<Item> oldItem, Optional<Item> newItem) {
        player.setWeight(player.getWeight() + computeWeightDifference(oldItem, newItem));
    }

    /**
     * Computes the weight difference for a single item set.
     */
    private double computeWeightDifference(Optional<Item> oldItem, Optional<Item> newItem) {
        double subtract = computeWeight(oldItem);
        double add = computeWeight(newItem);
        return add - subtract;
    }

    /**
     * Computes the weight of {@code item}.
     */
    private double computeWeight(Optional<Item> item) {
        return item.map(Item::getItemDef).
            map(ItemDefinition::getWeight).
            orElse(0.0);
    }
}
