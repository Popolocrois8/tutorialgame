package com.tutorial.game.gameComponenets.gameObjects;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.tutorial.game.gameComponenets.screens.GameScreen;

public class ScrollCollected {
    Texture scrollTxr;
    Texture pinTxt;
    Sprite scrollSprite;
    Sprite pinSprite;
    GameScreen gs;

    String attackDirection;

    public ScrollCollected(GameScreen gs, String attackDirection) {
        switch (attackDirection) {
            case "up":
                scrollTxr = new Texture("scroll_up.png");
                break;
            case "down":
                scrollTxr = new Texture("scroll_down.png");
                break;
            case "left":
                scrollTxr = new Texture("scroll_left.png");
                break;
            case "right":
                scrollTxr = new Texture("scroll_right.png");
                break;
            default:
                scrollTxr = new Texture("pin.png");
                break;
        }
        this.attackDirection = attackDirection;
        pinTxt = new Texture("pin.png");
        scrollSprite = new Sprite(scrollTxr);
        pinSprite = new Sprite(pinTxt);
        scrollSprite.setSize(6.5f, 6.5f);
        pinSprite.setSize(1, 1);
        this.gs = gs;
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
        if (gs.getMouseOnScroll() == this) {
            scrollSprite.setSize(6.8f, 6.8f);
            scrollSprite.draw(batch);
        } else {
            scrollSprite.setSize(6.5f, 6.5f);
            scrollSprite.draw(batch);
            pinSprite.draw(batch);
        }
    }

    public Sprite getScrollSprite() {
        return scrollSprite;
    }

    public String getAttackDirection(){
        return attackDirection;
    }

}
