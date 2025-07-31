package com.cavetale.endergolf;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import static com.cavetale.core.util.CamelCase.toCamelCase;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;

public final class GolfScoringTerm {
    @Getter
    @RequiredArgsConstructor
    private enum UnderPar {
        BIRDIE(1),
        EAGLE(2),
        ALBATROSS(3),
        CONDOR(4);

        private final int difference;
    }

    @Getter
    @RequiredArgsConstructor
    private enum OverPar {
        BOGEY(1),
        DOUBLE_BOGEY(2),
        TRIPLE_BOGEY(3),
        QUADRUPLE_BOGEY(4),
        QUINTUPLE_BOGEY(5),
        ;

        private final int difference;
    }

    public static Component getComponent(int strokes, int par) {
        if (strokes == 1) {
            return text("Ace", GOLD, BOLD);
        } else if (strokes == par) {
            return text("Par", YELLOW);
        } else if (strokes < par) {
            final int diff = par - strokes;
            for (UnderPar underPar : UnderPar.values()) {
                if (underPar.difference == diff) {
                    return text(toCamelCase(" ", underPar), GREEN);
                }
            }
            return text(diff + " Under Par", GREEN);
        } else {
            final int diff = strokes - par;
            for (OverPar overPar : OverPar.values()) {
                if (overPar.difference == diff) {
                    return text(toCamelCase(" ", overPar), RED);
                }
            }
            return text(diff + " Over Par", RED);
        }
    }

    private GolfScoringTerm() { }
}
