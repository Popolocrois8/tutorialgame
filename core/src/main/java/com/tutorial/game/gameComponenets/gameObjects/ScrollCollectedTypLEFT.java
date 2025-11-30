package com.tutorial.game.gameComponenets.gameObjects;

import com.tutorial.game.gameComponenets.screens.GameScreen;

public class ScrollCollectedTypLEFT extends ScrollCollected{
    public ScrollCollectedTypLEFT(GameScreen gs) {
        super(gs, "scroll_a.png");
    }

    @Override
    public String getAttackDirection() {
        return "left";
    }
}
