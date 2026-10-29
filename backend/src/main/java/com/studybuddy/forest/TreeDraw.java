package com.studybuddy.forest;

/** The random parts of a tree: which kind it is within its size tier, and where it stands. */
public record TreeDraw(int variant, int plot) {
    public TreeDraw {
        if (variant < 0 || plot < 0) throw new IllegalArgumentException("Tree draws are never negative");
    }
}
