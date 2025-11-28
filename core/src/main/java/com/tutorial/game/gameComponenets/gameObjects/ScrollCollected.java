package com.tutorial.game.gameComponenets.gameObjects;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class ScrollCollected {
    Texture scrollTxr;
    Sprite scrollSprite;
    public ScrollCollected() {
        scrollTxr = new Texture("scroll_1.png");
        scrollSprite = new Sprite(scrollTxr);
        scrollSprite.setSize(6, 6);
    }

    public void draw(SpriteBatch batch, int i){
        switch (i) {
            case 0:
                scrollSprite.setPosition(36,24);
                break;
            case 1:
                scrollSprite.setPosition(40,22);
                break;
            case 2:
                scrollSprite.setPosition(44,24);
                break;
            case 3:
                scrollSprite.setPosition(48,22);
                break;
        }
        scrollSprite.draw(batch);
    }
}
