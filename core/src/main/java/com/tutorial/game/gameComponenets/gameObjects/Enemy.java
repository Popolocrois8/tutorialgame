package com.tutorial.game.gameComponenets.gameObjects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.tutorial.game.gameComponenets.screens.GameScreen;

import static com.badlogic.gdx.math.MathUtils.random;

public class Enemy {
    float summitPercentage = 0.5f;

    Texture enemyTxr;
    Sprite enemySprite;
    boolean verticalWalking;
    float goal = newGoal();
    float speed = (float) (0.04+Math.random()*0.02);
    float movementStopTimer;
    String direction;

    public Enemy(int x){
        enemyTxr = new Texture("enemy.png");
        enemySprite = new Sprite(enemyTxr);
        enemySprite.setSize(2.6f,2.6f);
        enemySprite.setOrigin(1.3f,1.3f);
        switch (x){
            case 0:
                enemySprite.setPosition(2-0.3f,15);
                enemySprite.setRotation(90);
                verticalWalking = true;
                direction = "right";
                break;
            case 1:
                enemySprite.setPosition(15,28-0.3f);
                enemySprite.setRotation(0);
                verticalWalking = false;
                direction = "down";
                break;
            case 2:
                enemySprite.setPosition(15,2-0.3f);
                enemySprite.setRotation(180);
                verticalWalking = false;
                direction = "up";
                break;
            case 3:
                enemySprite.setPosition(28-0.3f,15);
                enemySprite.setRotation(270);
                verticalWalking = true;
                direction = "left";
                break;
        }
    }
    public void draw(SpriteBatch batch){
        enemySprite.draw(batch);
    }

    public void update(GameScreen gs){
        if (movementStopTimer == 0) {
            if (verticalWalking) {
                if (goal <= enemySprite.getY() + speed / 2 && goal >= enemySprite.getY() - speed / 2) {
                    goal = newGoal();
                    speed = newSpeed();
                    movementStopTimer = 1f;
                    //Wahrscheinlichkeit zum Angreifen hier festlegen evtl mit Faktor der ueber zeit schwerer macht
                    if(random() <= summitPercentage) {
                        gs.addIceAttack(enemySprite.getX(), enemySprite.getY(), direction);
                    }
                }
                if (enemySprite.getY() < goal) {
                    enemySprite.setPosition(enemySprite.getX(), enemySprite.getY() + speed);
                } else {
                    enemySprite.setPosition(enemySprite.getX(), enemySprite.getY() - speed);
                }
            }else{
                if (goal <= enemySprite.getX() + speed / 2 && goal >= enemySprite.getX() - speed / 2) {
                    goal = newGoal();
                    speed = newSpeed();
                    movementStopTimer = 1.5f;
                    //Wahrscheinlichkeit zum Angreifen hier festlegen evtl mit Faktor der ueber zeit schwerer macht
                    if(random() <= summitPercentage) {
                        gs.addIceAttack(enemySprite.getX(), enemySprite.getY(), direction);
                    }
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

    private float newGoal(){
        return (float) (6.0 + Math.random() * 18.0);
    }

    private float newSpeed(){
        return (float) (0.03 + Math.random() * 0.02);
    }

    public Sprite getSprite(){
        return enemySprite;
    }
}
