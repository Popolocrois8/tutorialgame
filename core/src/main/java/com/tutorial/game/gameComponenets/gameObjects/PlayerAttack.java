package com.tutorial.game.gameComponenets.gameObjects;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.tutorial.game.gameComponenets.screens.GameScreen;

public class PlayerAttack {
    String direction;
    Sprite attackSprite;
    float speed = 0.4f;
    GameScreen gameScreen;

    public PlayerAttack(GameScreen gs, String direction) {
        this.direction = direction;
        this.gameScreen = gs;
        attackSprite = new Sprite(new Texture("ice_orb.png"));
        attackSprite.setSize(2, 2);
        attackSprite.setOrigin(1, 1);
        attackSprite.setPosition(gameScreen.getPlayerSprite().getX(), gameScreen.getPlayerSprite().getY());
    }

    public void update(){
        float x = attackSprite.getX();
        float y = attackSprite.getY();

        Enemy[] enemies = gameScreen.getEnemies();

        for(Enemy enemy : enemies){
            if(enemy != null){
                Sprite eSpr = enemy.getSprite();
                if(x<=eSpr.getX()+eSpr.getWidth() && x+attackSprite.getWidth()>=eSpr.getX() && y<=eSpr.getY()+eSpr.getHeight() &&y+attackSprite.getHeight()>=eSpr.getY()){
                    gameScreen.removeEnemy(enemy);
                    gameScreen.removePlayerAttack(this);
                    break;
                }
            }
        }

        if(x <= 1 || y <= 1 || x >= 30-1 || y >= 30-1){
            gameScreen.removePlayerAttack(this);
        }else{
            switch (direction){
                case "right":
                        attackSprite.translateX(speed);
                    break;
                case "left":
                        attackSprite.translateX(-speed);
                    break;
                case "up":
                        attackSprite.translateY(speed);
                    break;
                case "down":
                        attackSprite.translateY(-speed);
                    break;
            }
        }
        attackSprite.rotate(10);
        }

    public void draw(SpriteBatch batch){
        attackSprite.draw(batch);
    }
}
