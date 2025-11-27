package com.tutorial.game.gameComponenets.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.tutorial.game.MainGame;
import com.tutorial.game.gameComponenets.gameObjects.Enemy;

import java.util.ArrayList;

public class GameScreen implements Screen {
    final MainGame game;

    Texture arenaTxr;
    Texture playerTxr;
    Sprite playerSprite;
    Texture scrollTxr;

    float enemyTimer;
    float spellDropTimer;

    int enemyCount = 0;
    Array<Enemy> enemies;

    public GameScreen(MainGame game) {
        this.game = game;
        arenaTxr = new Texture("arena.png");
        playerTxr = new Texture("player.png");
        scrollTxr = new Texture("spell.png");
        playerSprite = new Sprite(playerTxr);
        playerSprite.setSize(2, 2);
        playerSprite.setPosition(15, 15);
        enemies = new Array<>();
        createEnemy();
    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {
        input();
        logic();
        draw();
    }

    private void input() {
        float speed = 8f;
        float delta = Gdx.graphics.getDeltaTime();

        //Later here the camera values will be used
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            playerSprite.translateX(speed * delta);
        }
        else if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            playerSprite.translateX(-speed * delta);
        }

        if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
            playerSprite.translateY(speed * delta);
        }
        else if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            playerSprite.translateY(-speed * delta);
        }
    }

    private void logic() {
        float worldWidth = game.viewport.getWorldWidth();
        float worldHeight = game.viewport.getWorldHeight();
        float playerWidth = playerSprite.getWidth();
        float playerHeight = playerSprite.getHeight();
        playerSprite.setX(MathUtils.clamp(playerSprite.getX(), 6, 26 - playerWidth));
        playerSprite.setY(MathUtils.clamp(playerSprite.getY(), 6, 26 - playerHeight));
        float delta = Gdx.graphics.getDeltaTime();

        enemyTimer += delta;
        if (enemyTimer > 2f) {
            createEnemy();
        }

        spellDropTimer += delta;
        if (spellDropTimer > 1f) {
            spellDropTimer = 0;
            //createDroplet();
        }

        for (Enemy enemy : enemies){
            enemy.update();
        }

    }

    private void draw() {
        ScreenUtils.clear(Color.BLACK);
        game.viewport.apply();
        game.batch.setProjectionMatrix(game.viewport.getCamera().combined);
        game.batch.begin();

        float worldWidth = game.viewport.getWorldWidth();
        float worldHeight = game.viewport.getWorldHeight();

        game.batch.draw(arenaTxr, 0, 0, worldWidth, worldHeight);
        playerSprite.draw(game.batch);

        for (Enemy enemy : enemies) {
            enemy.draw(game.batch);
        }

        game.batch.end();
    }

    private void createEnemy() {
        if (enemyCount <= 3) {
            enemies.add(new Enemy(enemyCount));
            enemyCount++;
            enemyTimer = 0;
            System.out.println("ENEMY ADDED");
        }
    }


    @Override
    public void resize(int width, int height) {
        game.viewport.update(width, height, true);
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {

    }
}
