package com.tutorial.game.gameComponenets.gameObjects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.tutorial.game.gameComponenets.screens.GameScreen;

import static com.badlogic.gdx.math.MathUtils.random;

public class EnemyAttack {
    Array<Sprite> attackSprites;
    float midX = 0.3f;
    float midY = 0.3f;
    float offsetCaster = 2;
    float offsetCollective;
    String direction;
    GameScreen gameScreen;
    float speed = 0.1f;
    float harmlessCounter;

    public EnemyAttack(GameScreen gs, float x, float y, String direction){
        harmlessCounter = 1;
        gameScreen=gs;
        this.direction=direction;
        attackSprites = new Array<>();
        x += midX;
        y += midY;

        for (int i = 0; i < 4; i++){
            attackSprites.add(new Sprite(new Texture("fire_swirl.png")));
            attackSprites.get(i).setSize(2, 2);
            attackSprites.get(i).setOrigin(1, 1);
        }
        offsetCollective = attackSprites.get(0).getWidth()/2;
        switch(direction){
            case "right":
                attackSprites.get(1).setPosition(x+offsetCaster,y+3*offsetCollective);
                attackSprites.get(2).setPosition(x+offsetCaster,y-3*offsetCollective);
                attackSprites.get(0).setPosition(x+offsetCaster,y+offsetCollective);
                attackSprites.get(3).setPosition(x+offsetCaster,y-offsetCollective);
                break;
            case "left":
                attackSprites.get(1).setPosition(x-offsetCaster,y+3*offsetCollective);
                attackSprites.get(2).setPosition(x-offsetCaster,y-3*offsetCollective);
                attackSprites.get(0).setPosition(x-offsetCaster,y+offsetCollective);
                attackSprites.get(3).setPosition(x-offsetCaster,y-offsetCollective);
                break;
            case "up":
                attackSprites.get(1).setPosition(x-3*offsetCollective,y+offsetCaster);
                attackSprites.get(2).setPosition(x+3*offsetCollective,y+offsetCaster);
                attackSprites.get(0).setPosition(x-offsetCollective,y+offsetCaster);
                attackSprites.get(3).setPosition(x+offsetCollective,y+offsetCaster);
                break;
            case "down":
                attackSprites.get(1).setPosition(x-3*offsetCollective,y-offsetCaster);
                attackSprites.get(2).setPosition(x+3*offsetCollective,y-offsetCaster);
                attackSprites.get(0).setPosition(x-offsetCollective,y-offsetCaster);
                attackSprites.get(3).setPosition(x+offsetCollective,y-offsetCaster);
                break;
        }
        attackSprites.get(1).setRotation(-90);
        attackSprites.get(1).setRotation(180);
        attackSprites.get(3).setRotation(90);
    }

    public void update(){
        if (harmlessCounter <= 0){
            float playerX = gameScreen.getPlayerSprite().getX();
            float playerY = gameScreen.getPlayerSprite().getY();
            float playerThic = gameScreen.getPlayerSprite().getHeight();
            boolean collisionFound = false;

            for(Sprite sprite : attackSprites){
                if (!collisionFound) {
                    float spriteX = sprite.getX();
                    float spriteY = sprite.getY();
                    float spriteThic = sprite.getHeight();

                    if(playerX <= spriteX+spriteThic && playerX+playerThic >= spriteX && playerY <= spriteY+spriteThic && playerY+playerThic >= spriteY){
                        gameScreen.removeIceAttack(this);
                        gameScreen.takeDamage();
                        collisionFound = true;
                    }
                }
            }

            if (!collisionFound){
                float x = attackSprites.get(0).getX();
                float y = attackSprites.get(0).getY();

                if(x <= 1 || y <= 1 || x >= 30-1 || y >= 30-1){
                    gameScreen.removeIceAttack(this);
                }else{
                    switch (direction){
                        case "right":
                            for (Sprite sprite : attackSprites) {
                                sprite.translateX(speed);
                            }
                            break;
                        case "left":
                            for (Sprite sprite : attackSprites){
                                sprite.translateX(-speed);
                            }
                            break;
                        case "up":
                            for (Sprite sprite : attackSprites){
                                sprite.translateY(speed);
                            }
                            break;
                        case "down":
                            for (Sprite sprite : attackSprites){
                                sprite.translateY(-speed);
                            }
                            break;
                    }
                }
                for (Sprite sprite : attackSprites){
                    sprite.rotate(10);
                }
            }
        }else{
            harmlessCounter -= Gdx.graphics.getDeltaTime();
        }

    }

    public void draw(SpriteBatch batch){
        for (Sprite sprite : attackSprites) {
            sprite.draw(batch);
        }
    }
}

/*
public class EnemyAttack {
    Array<Sprite> attackSprites;
    float midX = 0.3f;
    float midY = 0.3f;
    float offsetCaster = 2;
    float offsetCollective = 2;
    String direction;
    GameScreen gameScreen;
    float speed = 0.1f;
    float harmlessCounter;

    public EnemyAttack(GameScreen gs, float x, float y, String direction){
        harmlessCounter = 1;
        gameScreen=gs;
        this.direction=direction;
        attackSprites = new Array<>();
        x += midX;
        y += midY;

        for (int i = 0; i < 3; i++){
            attackSprites.add(new Sprite(new Texture("fire_swirl.png")));
            attackSprites.get(i).setSize(2, 2);
            attackSprites.get(i).setOrigin(1, 1);
        }
        switch(direction){
            case "right":
                attackSprites.get(0).setPosition(x+offsetCaster,y);
                attackSprites.get(1).setPosition(x+offsetCaster,y+offsetCollective);
                attackSprites.get(2).setPosition(x+offsetCaster,y-offsetCollective);
                break;
            case "left":
                attackSprites.get(0).setPosition(x-offsetCaster,y);
                attackSprites.get(1).setPosition(x-offsetCaster,y+offsetCollective);
                attackSprites.get(2).setPosition(x-offsetCaster,y-offsetCollective);
                break;
            case "up":
                attackSprites.get(0).setPosition(x,y+offsetCaster);
                attackSprites.get(1).setPosition(x-offsetCollective,y+offsetCaster);
                attackSprites.get(2).setPosition(x+offsetCollective,y+offsetCaster);
                break;
            case "down":
                attackSprites.get(0).setPosition(x,y-offsetCaster);
                attackSprites.get(1).setPosition(x-offsetCollective,y-offsetCaster);
                attackSprites.get(2).setPosition(x+offsetCollective,y-offsetCaster);
                break;
        }
        attackSprites.get(1).setRotation(-90);
        attackSprites.get(1).setRotation(180);
    }

    public void update(){
        if (harmlessCounter <= 0){
            float playerX = gameScreen.getPlayerSprite().getX();
            float playerY = gameScreen.getPlayerSprite().getY();
            float playerThic = gameScreen.getPlayerSprite().getHeight();
            boolean collisionFound = false;

            for(Sprite sprite : attackSprites){
                if (!collisionFound) {
                    float spriteX = sprite.getX();
                    float spriteY = sprite.getY();
                    float spriteThic = sprite.getHeight();

                    if(playerX <= spriteX+spriteThic && playerX+playerThic >= spriteX && playerY <= spriteY+spriteThic && playerY+playerThic >= spriteY){
                        gameScreen.removeIceAttack(this);
                        gameScreen.takeDamage();
                        collisionFound = true;
                    }
                }
            }

            if (!collisionFound){
                float x = attackSprites.get(0).getX();
                float y = attackSprites.get(0).getY();

                if(x <= 1 || y <= 1 || x >= 30-1 || y >= 30-1){
                    gameScreen.removeIceAttack(this);
                }else{
                    switch (direction){
                        case "right":
                            for (Sprite sprite : attackSprites) {
                                sprite.translateX(speed);
                            }
                            break;
                        case "left":
                            for (Sprite sprite : attackSprites){
                                sprite.translateX(-speed);
                            }
                            break;
                        case "up":
                            for (Sprite sprite : attackSprites){
                                sprite.translateY(speed);
                            }
                            break;
                        case "down":
                            for (Sprite sprite : attackSprites){
                                sprite.translateY(-speed);
                            }
                            break;
                    }
                }
                for (Sprite sprite : attackSprites){
                        sprite.rotate(10);
                }
            }
        }else{
            harmlessCounter -= Gdx.graphics.getDeltaTime();
        }

    }

    public void draw(SpriteBatch batch){
        for (Sprite sprite : attackSprites) {
            sprite.draw(batch);
        }
    }
}
*/
