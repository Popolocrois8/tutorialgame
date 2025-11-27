package com.tutorial.game.gameComponenets.gameObjects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class Enemy {
    Texture enemyTxr;
    Sprite enemySprite;
    boolean verticalWalking;
    int goal = (int) (6.0+Math.random()*24.0);
    float speed = (float) (0.04+Math.random()*0.02);
    float movementStopTimer;

    public Enemy(int x){
        enemyTxr = new Texture("enemy.png");
        enemySprite = new Sprite(enemyTxr);
        enemySprite.setOrigin(1,1);
        enemySprite.setSize(2,2);
        switch (x){
            case 0:
                enemySprite.setPosition(2,15);
                enemySprite.setRotation(90);
                verticalWalking = true;
                break;
            case 1:
                enemySprite.setPosition(15,28);
                enemySprite.setRotation(0);
                verticalWalking = false;
                break;
            case 2:
                enemySprite.setPosition(15,2);
                enemySprite.setRotation(180);
                verticalWalking = false;
                break;
            case 3:
                enemySprite.setPosition(28,15);
                enemySprite.setRotation(270);
                verticalWalking = true;
                break;
        }
    }
    public void draw(SpriteBatch batch){
        enemySprite.draw(batch);
    }

    public void update(){
        if (movementStopTimer == 0) {
            if (verticalWalking) {
                if (goal <= enemySprite.getY() + speed / 2 && goal >= enemySprite.getY() - speed / 2) {
                    System.out.println("Goal erreicht");
                    goal = (int) (6.0 + Math.random() * 18.0);
                    speed = (float) (0.04 + Math.random() * 0.01);
                    movementStopTimer = 1f;
                    //Wahrscheinlichkeit zum Angreifen hier festlegen evtl mit Faktor der ueber zeit schwerer macht
                }
                if (enemySprite.getY() < goal) {
                    enemySprite.setPosition(enemySprite.getX(), enemySprite.getY() + speed);
                } else {
                    enemySprite.setPosition(enemySprite.getX(), enemySprite.getY() - speed);
                }
            }else{
                if (goal <= enemySprite.getX() + speed / 2 && goal >= enemySprite.getX() - speed / 2) {
                    System.out.println("Goal erreicht");
                    goal = (int) (6.0 + Math.random() * 18.0);
                    speed = (float) (0.04 + Math.random() * 0.02);
                    movementStopTimer = 1.5f;
                    //Wahrscheinlichkeit zum Angreifen hier festlegen evtl mit Faktor der ueber zeit schwerer macht
                }
                if (enemySprite.getX() < goal) {
                    enemySprite.setPosition(enemySprite.getX() + speed, enemySprite.getY());
                } else {
                    enemySprite.setPosition(enemySprite.getX() - speed, enemySprite.getY());
                }

            }
        } else if (movementStopTimer >= 0) {
            movementStopTimer -= Gdx.graphics.getDeltaTime();
        } else {
            movementStopTimer = 0;
        }

    }
}
