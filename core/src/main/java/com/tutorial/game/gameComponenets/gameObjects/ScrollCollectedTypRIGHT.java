package com.tutorial.game.gameComponenets.gameObjects;

import com.tutorial.game.gameComponenets.screens.GameScreen;

public class ScrollCollectedTypRIGHT extends ScrollCollected{
    public ScrollCollectedTypRIGHT(GameScreen gs) {
        super(gs, "scroll_b.png");
    }

    @Override
    public String getAttackDirection() {
        return "right";
    }
}
