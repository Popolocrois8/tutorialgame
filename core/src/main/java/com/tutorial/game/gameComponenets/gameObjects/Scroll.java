package com.tutorial.game.gameComponenets.gameObjects;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import static com.badlogic.gdx.math.MathUtils.random;

public class Scroll {
    Texture scrollTxr;
    Sprite scrollSprite;

    public Scroll() {
        scrollTxr = new Texture("scroll.png");
        scrollSprite = new Sprite(scrollTxr);
        scrollSprite.setSize(2f, 2f);
        scrollSprite.setPosition(6+random()*(20-scrollSprite.getWidth()),6+random()*(20-scrollSprite.getHeight()));
    }

    public void draw(SpriteBatch batch){
        scrollSprite.draw(batch);
    }

    public Sprite getScrollSprite() {
        return scrollSprite;
    }
}
