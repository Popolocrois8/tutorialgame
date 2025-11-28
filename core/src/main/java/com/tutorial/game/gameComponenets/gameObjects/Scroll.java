package com.tutorial.game.gameComponenets.gameObjects;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import static com.badlogic.gdx.math.MathUtils.random;

public class Scroll {
    Texture scrollTxr;
    Sprite scrollSprite;

    public Scroll() {
        if (random() < 0.5){
            scrollTxr = new Texture("scroll_1.png");
        }else{
            scrollTxr = new Texture("scroll_2.png");
        }
        scrollSprite = new Sprite(scrollTxr);
        scrollSprite.setSize(1.5f, 1.5f);
        scrollSprite.setPosition(6+random()*(20-scrollSprite.getWidth()),6+random()*(20-scrollSprite.getHeight()));
    }

    public void draw(SpriteBatch batch){
        scrollSprite.draw(batch);
    }

    public Sprite getScrollSprite() {
        return scrollSprite;
    }
}
