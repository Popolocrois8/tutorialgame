package com.tutorial.game.gameComponenets.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.tutorial.game.MainGame;
import com.tutorial.game.gameComponenets.gameObjects.*;
import com.tutorial.game.gameComponenets.utils.InputUtils;
import com.tutorial.game.gameComponenets.controllers.HeadMovementController;

import static com.badlogic.gdx.math.MathUtils.random;

public class GameScreen implements Screen {
    final MainGame game;
    float survivalTime;

    boolean attackSeq = false;
    String tempAttackDirection;

    Texture arenaTxr;
    Texture playerTxr;
    Sprite playerSprite;
    Texture heartTxr;

    float enemyTimer;
    // TODO evtl hier mit der Zeit abaenderbar
    float enemySpawnTime = 3f;
    float scrollSpawnTimer = 0.4f;
    float attackDurationTimer;
    final float MAX_attackDurationTimer = 2f;

    Array<Sprite> hearts;
    Enemy[] enemies;
    Array<Scroll> scrolls;
    Array<ScrollCollected> scrollsCollected;
    Array<EnemyAttack> enemyAttacks;
    Array<PlayerAttack> playerAttacks;
    final int MAX_SCROLLS = 2;
    Array<Sprite> spritesForAttackSeq;

    ScrollCollected mouseOnScroll = null;

    Texture pixel;

    // Head movement controller
    private HeadMovementController headController;
    private boolean useHeadControl = false;
    private Texture cameraTexture;

    public GameScreen(MainGame game) {
        this.game = game;
        arenaTxr = new Texture("arena.png");
        playerTxr = new Texture("player.png");
        heartTxr = new Texture("heart.png");
        playerSprite = new Sprite(playerTxr);
        playerSprite.setSize(2.4f, 2.4f);
        playerSprite.setPosition(15, 15);
        enemies = new Enemy[4];
        scrolls =  new Array<>();
        scrollsCollected = new Array<>();
        hearts = new Array<>();
        enemyAttacks = new Array<>();
        playerAttacks = new Array<>();
        spritesForAttackSeq =  new Array<>();
        spritesForAttackSeq.add(new Sprite(new Texture("timer_frame.png")));
        spritesForAttackSeq.get(0).setSize(58,32);
        spritesForAttackSeq.get(0).setPosition(0,0);
        spritesForAttackSeq.add(new Sprite(new Texture("timer_bar.png")));
        spritesForAttackSeq.get(1).setPosition(35,24);
        //createEnemy();
        setHearts();

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(1, 1, 1, 1); // white pixel
        pixmap.fill();
        pixel = new Texture(pixmap);
        pixmap.dispose();

        // Initialize head movement controller
        headController = new HeadMovementController();
        cameraTexture = new Texture(1, 1, Pixmap.Format.RGB888); // placeholder

    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {
        if(!attackSeq) {
            survivalTime += Gdx.graphics.getDeltaTime();
            input();
            logic();
        }else{
            inputWhenPaused();
            logicWhenPaused();
        }
        draw();
    }

    private void inputWhenPaused() {

    }

    private void logicWhenPaused() {
        attackDurationTimer -= Gdx.graphics.getDeltaTime();
        float timerBarProgress = 0;
        if(attackDurationTimer > 0) {
            timerBarProgress = attackDurationTimer / MAX_attackDurationTimer;
            if(attackSucceeded()){
                playerAttacks.add(new PlayerAttack(this,tempAttackDirection));
                pauseOrResumeGameForAttack();
            }
        }else{
            // TODO hier Failergergebnis einbauen falls was konkret passieren soll
            pauseOrResumeGameForAttack();
        }
        spritesForAttackSeq.get(1).setSize(20*timerBarProgress,4);
    }

    private void drawWhenPaused() {
        game.batch.setColor(0, 0, 0, 0.65f); // semi-transparent black
        game.batch.draw(pixel, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        game.batch.setColor(1, 1, 1, 1); // reset color
        for (Sprite sprite : spritesForAttackSeq) {
            sprite.draw(game.batch);
        }
    }

    private void input() {
        float speed = 8f;
        float delta = Gdx.graphics.getDeltaTime();

        // Toggle control methods with H key
        if (Gdx.input.isKeyJustPressed(Input.Keys.H)) {
            useHeadControl = !useHeadControl;
            if (useHeadControl && headController.isHeadTrackingEnabled()) {
                headController.recalibrate();
                System.out.println("Head control ENABLED");
            } else {
                System.out.println("Keyboard control ENABLED");
            }
        }

        // Toggle camera feed with C key
        if (Gdx.input.isKeyJustPressed(Input.Keys.C)) {
            headController.toggleCameraFeed();
        }

        // Recalibrate head position with R key
        if (Gdx.input.isKeyJustPressed(Input.Keys.R) && useHeadControl) {
            headController.recalibrate();
        }

        if (useHeadControl && headController.isHeadTrackingEnabled() && !attackSeq) {
            // Head movement control
            headController.updateHeadPosition();
            float headX = headController.getHorizontalMovement();
            float headY = headController.getVerticalMovement();

            playerSprite.translateX(headX * speed * delta);
            playerSprite.translateY(headY * speed * delta);

        } else {
            // Original keyboard control (fallback)
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

        Vector2 mouseCoords = InputUtils.getMouseWorldCoords(game.viewport);
        mouseOnScroll = null;
        for (ScrollCollected scroll : scrollsCollected) {
            Sprite sspr = scroll.getScrollSprite();
            if(mouseCoords.x <= sspr.getX()+sspr.getWidth() && mouseCoords.x >= sspr.getX() && mouseCoords.y <= sspr.getY()+sspr.getWidth() && mouseCoords.y >= sspr.getY()){
                mouseOnScroll = scroll;
                if(Gdx.input.isTouched()){
                    // TODO hier kann mit scroll.abstractMethod zwischen den AttackTypen unterschieden werden
                    tempAttackDirection = scroll.getAttackDirection();
                    scrollsCollected.removeValue(scroll,true);
                    attackDurationTimer = MAX_attackDurationTimer;
                    pauseOrResumeGameForAttack();
                }
                break;
            }
        }
    }

    private void logic() {
        float playerWidth = playerSprite.getWidth();
        float playerHeight = playerSprite.getHeight();
        playerSprite.setX(MathUtils.clamp(playerSprite.getX(), 6, 26 - playerWidth));
        playerSprite.setY(MathUtils.clamp(playerSprite.getY(), 6, 26 - playerHeight));
        float delta = Gdx.graphics.getDeltaTime();

        enemyTimer += delta;
        if (enemyTimer > enemySpawnTime) {
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
            if(enemy != null) {
                enemy.update(this);
            }
        }

        for (PlayerAttack playerAttack : playerAttacks){
            playerAttack.update();
        }

        for (int i = scrolls.size - 1; i >= 0; i--) {
            Sprite scrollSprite = scrolls.get(i).getScrollSprite();
            float scrollWidth = scrollSprite.getWidth();
            float scrollHeight = scrollSprite.getHeight();

            if (playerSprite.getX() + playerWidth >= scrollSprite.getX() && playerSprite.getX() <= scrollSprite.getX() + scrollWidth && playerSprite.getY() + playerHeight >= scrollSprite.getY() && playerSprite.getY() <= scrollSprite.getY() + scrollHeight && scrollsCollected.size < 4) {
                scrolls.removeIndex(i);
                addNewScrollCollected();
            }
        }

        // Update camera texture
        if (headController.isHeadTrackingEnabled() && headController.isCameraFeedEnabled()) {
            Texture newTexture = headController.getCameraTexture();
            if (newTexture != null) {
                cameraTexture = newTexture;
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
            if (enemy != null) {
                enemy.draw(game.batch);
            }
        }

        for (EnemyAttack enemyAttack : enemyAttacks) {
            enemyAttack.draw(game.batch);
        }

        for (Sprite heart : hearts) {
            heart.draw(game.batch);
        }

        for (PlayerAttack playerAttack : playerAttacks){
            playerAttack.draw(game.batch);
        }

        for (int i = scrollsCollected.size - 1; i >= 0; i--) {
            scrollsCollected.get(i).draw(game.batch,i);
        }

        playerSprite.draw(game.batch);

        // Draw camera feed
        if (headController.isHeadTrackingEnabled() && headController.isCameraFeedEnabled() && cameraTexture != null && !attackSeq) {
            // Draw in top-right corner, scaled down
            float cameraWidth = 8f;
            float cameraHeight = 6f;
            float cameraX = worldWidth - cameraWidth - 1f;
            float cameraY = worldHeight - cameraHeight - 1f;

            game.batch.draw(cameraTexture, cameraX, cameraY, cameraWidth, cameraHeight);
        }

        if(attackSeq) {
            drawWhenPaused();
        }

        // Draw control info
        if (game.font != null && !attackSeq) {
            float yPos = worldHeight - 20;
            String controlMethod = useHeadControl ? "HEAD CONTROL" : "KEYBOARD CONTROL";
            game.font.draw(game.batch, "Control: " + controlMethod, 10, yPos);
            yPos -= 15;
            game.font.draw(game.batch, "Press H: Toggle control", 10, yPos);
            yPos -= 15;
            game.font.draw(game.batch, "Press C: Toggle camera", 10, yPos);
            if (useHeadControl) {
                yPos -= 15;
                game.font.draw(game.batch, "Press R: Recalibrate", 10, yPos);
            }
        }

        game.batch.end();
    }

    private void createEnemy() {
        for (int i = 0; i < enemies.length; i++){
            if (enemies[i] == null) {
                enemies[i] = new Enemy(i);
                enemyTimer = 0;
                break;
            }
        }
    }

    private void addNewScrollCollected() {
        int scrollType = (int) (random()*4);
        switch (scrollType){
            case 0:
                scrollsCollected.add(new ScrollCollectedTypUP(this));
                break;
            case 1:
                scrollsCollected.add(new ScrollCollectedTypRIGHT(this));
                break;
            case 2:
                scrollsCollected.add(new ScrollCollectedTypDOWN(this));
                break;
            case 3:
                scrollsCollected.add(new ScrollCollectedTypLEFT(this));
                break;
        }
    }

    public void takeDamage(){
        hearts.pop();
        if (hearts.size == 0) {
            game.setScreen(new EndScreen(game, survivalTime));
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

    public void removePlayerAttack(PlayerAttack identity){
        for (int i = playerAttacks.size - 1; i >= 0; i--) {
            if (playerAttacks.get(i).equals(identity)) {
                playerAttacks.removeIndex(i);
            }
        }
    }

    public Sprite getPlayerSprite(){
        return playerSprite;
    }

    public ScrollCollected getMouseOnScroll(){
        return mouseOnScroll;
    }

    public void pauseOrResumeGameForAttack(){
        attackSeq = !attackSeq;
    }

    private boolean attackSucceeded(){
        // TODO hier logik einbauen wann Attack erfolgreich
        if (Gdx.input.isKeyPressed(Input.Keys.SPACE)) {
            return true;
        }
        return false;
    }

    public Enemy[] getEnemies(){
        return enemies;
    }

    public void removeEnemy(Enemy identity){
        for (int i = enemies.length - 1; i >= 0; i--) {
            if (enemies[i] == identity) {
                enemies[i] = null;
                enemyTimer = 0;
                break;
            }
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
        arenaTxr.dispose();
        playerTxr.dispose();
        heartTxr.dispose();
        pixel.dispose();
        if (headController != null) {
            headController.dispose();
        }
        if (cameraTexture != null) {
            cameraTexture.dispose();
        }
    }
}
