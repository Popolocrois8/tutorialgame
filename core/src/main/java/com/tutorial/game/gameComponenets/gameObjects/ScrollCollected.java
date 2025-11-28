package com.tutorial.game.gameComponenets.gameObjects;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class ScrollCollected {
    Texture scrollTxr;
    Texture pinTxt;
    Sprite scrollSprite;
    Sprite pinSprite;
    public ScrollCollected() {
        scrollTxr = new Texture("scroll.png");
        pinTxt = new Texture("pin.png");
        scrollSprite = new Sprite(scrollTxr);
        pinSprite = new Sprite(pinTxt);
        scrollSprite.setSize(6.5f, 6.5f);
        pinSprite.setSize(1, 1);
    }

    public void draw(SpriteBatch batch, int i){
        switch (i) {
            case 0:
                scrollSprite.setPosition(32.8f,24.5f);
                break;
            case 1:
                scrollSprite.setPosition(39.05f,21.5f);
                break;
            case 2:
                scrollSprite.setPosition(45.3f,23.75f);
                break;
            case 3:
                scrollSprite.setPosition(51.3f,21f);
                break;
        }
        pinSprite.setPosition(scrollSprite.getX()+3.2f,scrollSprite.getY()+5.1f);
        scrollSprite.draw(batch);
        pinSprite.draw(batch);
    }
}
