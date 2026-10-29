package com.studybuddy.forest;

import java.util.random.RandomGenerator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Draws each new visit's tree kind and meadow plot at random on the server, so every student's
 * forest is different and a browser cannot choose a rarer tree.
 */
@Component
public class TreeLottery {
    /** Plots are spread over a large range; the meadow maps them onto however many cells it shows. */
    static final int PLOT_RANGE = 1_000_000;
    private final RandomGenerator random;
    private final int variants;

    public TreeLottery(@Qualifier("forestRandom") RandomGenerator random, ForestProperties properties) {
        this.random = random;
        this.variants = properties.variantsPerSpecies();
    }

    public TreeDraw draw() {
        return new TreeDraw(random.nextInt(variants), random.nextInt(PLOT_RANGE));
    }
}
