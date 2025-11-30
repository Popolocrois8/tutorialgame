package com.tutorial.game.gameComponenets.gameObjects;

import com.tutorial.game.gameComponenets.screens.GameScreen;

public class ScrollCollectedTypDOWN extends ScrollCollected{
    public ScrollCollectedTypDOWN(GameScreen gs) {
        super(gs, "scroll_a.png");
    }

    @Override
    public String getAttackDirection() {
        return "down";
    }
}
