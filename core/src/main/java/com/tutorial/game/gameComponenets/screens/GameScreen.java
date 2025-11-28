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
import com.tutorial.game.gameComponenets.gameObjects.EnemyAttack;
import com.tutorial.game.gameComponenets.gameObjects.Scroll;
import com.tutorial.game.gameComponenets.gameObjects.ScrollCollected;

public class GameScreen implements Screen {
    final MainGame game;

    Texture arenaTxr;
    Texture playerTxr;
    Sprite playerSprite;
    Texture heartTxr;

    float enemyTimer;
    float scrollSpawnTimer = 0.4f;

    Array<Sprite> hearts;
    Array<Enemy> enemies;
    Array<Scroll> scrolls;
    Array<ScrollCollected> scrollsCollected;
    Array<EnemyAttack> enemyAttacks;
    final int MAX_SCROLLS = 2;

    public GameScreen(MainGame game) {
        this.game = game;
        arenaTxr = new Texture("arena.png");
        playerTxr = new Texture("player.png");
        heartTxr = new Texture("heart.png");
        playerSprite = new Sprite(playerTxr);
        playerSprite.setSize(2.4f, 2.4f);
        playerSprite.setPosition(15, 15);
        enemies = new Array<>();
        scrolls =  new Array<>();
        scrollsCollected = new Array<>();
        hearts = new Array<>();
        enemyAttacks = new Array<>();
        createEnemy();
        setHearts();
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
        float playerWidth = playerSprite.getWidth();
        float playerHeight = playerSprite.getHeight();
        playerSprite.setX(MathUtils.clamp(playerSprite.getX(), 6, 26 - playerWidth));
        playerSprite.setY(MathUtils.clamp(playerSprite.getY(), 6, 26 - playerHeight));
        float delta = Gdx.graphics.getDeltaTime();

        enemyTimer += delta;
        if (enemyTimer > 2f) {
            createEnemy();
        }
        if (scrolls.size < MAX_SCROLLS) {
            scrollSpawnTimer -= delta;
        }
        if (scrollSpawnTimer <= 0){
            scrolls.add(new Scroll());
            scrollSpawnTimer = 2f;
        }

        for (EnemyAttack enemyAttack : enemyAttacks){
            enemyAttack.update();
        }

        for (Enemy enemy : enemies){
            enemy.update(this);
        }

        for (int i = scrolls.size - 1; i >= 0; i--) {
            Sprite scrollSprite = scrolls.get(i).getScrollSprite();
            float scrollWidth = scrollSprite.getWidth();
            float scrollHeight = scrollSprite.getHeight();

            if (playerSprite.getX() + playerWidth >= scrollSprite.getX() && playerSprite.getX() <= scrollSprite.getX() + scrollWidth && playerSprite.getY() + playerHeight >= scrollSprite.getY() && playerSprite.getY() <= scrollSprite.getY() + scrollHeight && scrollsCollected.size < 4) {
                scrolls.removeIndex(i);
                scrollsCollected.add(new ScrollCollected());
            }
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

        for (Scroll scroll : scrolls) {
            scroll.draw(game.batch);
        }

        for (Enemy enemy : enemies) {
            enemy.draw(game.batch);
        }

        for (EnemyAttack enemyAttack : enemyAttacks) {
            enemyAttack.draw(game.batch);
        }

        for (Sprite heart : hearts) {
            heart.draw(game.batch);
        }

        for (int i = scrollsCollected.size - 1; i >= 0; i--) {
            scrollsCollected.get(i).draw(game.batch,i);
        }

        playerSprite.draw(game.batch);

        game.batch.end();
    }

    private void createEnemy() {
        if (enemies.size <= 3) {
            enemies.add(new Enemy(enemies.size));
            enemyTimer = 0;
            System.out.println("ENEMY ADDED");
        }
    }

    public void takeDamage(){
        hearts.pop();
        if (hearts.size == 0) {
            game.setScreen(new EndScreen(game));
            dispose();
        }
    }

    private void setHearts(){
        for(int i = 0; i < 3; i++){
            hearts.add(new Sprite(heartTxr));
            hearts.get(i).setSize(5,5);
        }
        hearts.get(0).setPosition(33,1);
        hearts.get(1).setPosition(33,7);
        hearts.get(2).setPosition(33,13);
    }

    public void addIceAttack(float x, float y, String direction){
        enemyAttacks.add(new EnemyAttack(this,x,y,direction));
    }

    public void removeIceAttack(EnemyAttack identity){
        for (int i = enemyAttacks.size - 1; i >= 0; i--) {
            if (enemyAttacks.get(i).equals(identity)) {
                enemyAttacks.removeIndex(i);
            }
        }
    }

    public Sprite getPlayerSprite(){
        return playerSprite;
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
