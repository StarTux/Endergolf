package com.cavetale.endergolf;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Data;

@Data
public final class SaveTag implements Serializable {
    private boolean pause;
    private boolean event;
    private Map<UUID, Integer> scores = new HashMap<>();

    public void addScore(UUID uuid, int value) {
        scores.put(uuid, scores.getOrDefault(uuid, 0) + value);
    }
}
