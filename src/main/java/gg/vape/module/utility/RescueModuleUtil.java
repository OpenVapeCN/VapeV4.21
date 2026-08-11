package gg.vape.module.utility;

import gg.vape.Vape;
import gg.vape.notification.Notification;
import gg.vape.notification.NotificationType;
import gg.vape.notification.TextNotificationContent;
import gg.vape.wrapper.impl.InventoryPlayer;
import java.awt.Color;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Shared helpers for the fall rescue modules (Clutch, AutoLadder, MLG). */
public final class RescueModuleUtil {
    public static final List<String> PREFERRED_BLOCK_NAMES = Collections.unmodifiableList(
            Arrays.asList("Wool", "Stone", "Wood Planks", "Red Sandstone",
                    "Stained Clay", "End Stone", "Obsidian"));

    private RescueModuleUtil() {
    }

    public static Color countColor(int count) {
        if (count >= 32) {
            return new Color(2, 190, 58);
        }
        if (count >= 16) {
            return new Color(255, 249, 18);
        }
        return new Color(255, 20, 20);
    }

    public static Notification updateFailNotification(Notification existing, String title,
                                                      String message, boolean forceUpdate) {
        boolean shouldEnqueue = false;
        boolean expired = existing != null && existing.isExpired();
        if (existing == null) {
            existing = new Notification(NotificationType.ALERT, title,
                    new TextNotificationContent(message), 0.0, 0.0, 3500L);
            shouldEnqueue = true;
        } else if (expired || forceUpdate) {
            shouldEnqueue = expired;
            TextNotificationContent content = (TextNotificationContent)existing.getContent();
            content.setText(message);
            existing.setDuration(3500L);
        }
        if (shouldEnqueue) {
            Vape.INSTANCE.getNotificationManager().enqueue(existing, false);
        }
        return existing;
    }

    public static int findPreferredSlot(InventoryPlayer inventory, List<Integer> validSlots,
                                        List<String> preferredNames) {
        for (String preferredName : preferredNames) {
            for (Integer slot : validSlots) {
                if (inventory.c(slot.intValue()).x().contains(preferredName)) {
                    return slot.intValue();
                }
            }
        }
        return validSlots.isEmpty() ? -1 : validSlots.get(0).intValue();
    }
}
