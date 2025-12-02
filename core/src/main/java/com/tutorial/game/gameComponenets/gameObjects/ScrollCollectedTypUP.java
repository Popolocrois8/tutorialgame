package com.tutorial.game.gameComponenets.gameObjects;

import com.tutorial.game.gameComponenets.screens.GameScreen;

public class ScrollCollectedTypUP extends ScrollCollected{
    public ScrollCollectedTypUP(GameScreen gs) {
        super(gs, "scroll_up.png");
    }

    @Override
    public String getAttackDirection() {
        return "up";
    }
}
