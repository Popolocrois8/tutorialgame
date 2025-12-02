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
import com.tutorial.game.gameComponenets.controllers.HandSignController;

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
    final float MAX_attackDurationTimer = 15f;

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

    private HandSignController handSignController;


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
        handSignController = new HandSignController();

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
        // Hand sign detection happens automatically in update()
        // Just check if we detected the correct sign
        if (handSignController != null && handSignController.isActive()) {
            String detectedDir = handSignController.getDetectedDirection();

            if (detectedDir != null && detectedDir.equals(tempAttackDirection)) {
                // Success! Cast the spell
                playerAttacks.add(new PlayerAttack(this, tempAttackDirection));
                pauseOrResumeGameForAttack();
                System.out.println("✓ Spell cast with hand sign: " + tempAttackDirection);
            }

            // Update hand sign controller
            handSignController.update();
        }
    }

    private void logicWhenPaused() {
        attackDurationTimer -= Gdx.graphics.getDeltaTime();
        float timerBarProgress = 0;

        if (attackDurationTimer > 0) {
            timerBarProgress = attackDurationTimer / MAX_attackDurationTimer;

            // If hand sign controller detected correct direction
            if (handSignController != null && handSignController.hasDetectedDirection()) {
                String detectedDir = handSignController.getDetectedDirection();
                if (detectedDir.equals(tempAttackDirection)) {
                    playerAttacks.add(new PlayerAttack(this, tempAttackDirection));
                    pauseOrResumeGameForAttack();
                }
            }
        } else {
            // Time's up
            pauseOrResumeGameForAttack();
        }

        spritesForAttackSeq.get(1).setSize(20 * timerBarProgress, 4);

        // Update hand sign controller during attack sequence
        if (handSignController != null && handSignController.isActive()) {
            handSignController.update();
        }
    }

    private void drawWhenPaused() {
        game.batch.setColor(0, 0, 0, 0.65f);
        game.batch.draw(pixel, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        game.batch.setColor(1, 1, 1, 1);

        // Draw hand sign camera feed
        if (handSignController != null && handSignController.isActive()) {
            float worldWidth = game.viewport.getWorldWidth();
            float worldHeight = game.viewport.getWorldHeight();

            // Draw camera feed in center
            float camWidth = 20f;
            float camHeight = 15f;
            float camX = (worldWidth - camWidth) / 2;
            float camY = (worldHeight - camHeight) / 2 + 5; // Slightly above center

            handSignController.draw(game.batch, camX, camY, camWidth, camHeight);

            // Draw instructions
            if (game.font != null) {
                game.font.draw(game.batch, "SHOW HAND SIGN TO CAMERA",
                    camX, camY + camHeight + 15);
                game.font.draw(game.batch, "Required: " + tempAttackDirection.toUpperCase(),
                    camX, camY + camHeight + 30);
                game.font.draw(game.batch, String.format("Time: %.1fs", handSignController.getTimeRemaining()),
                    camX, camY - 10);

                // Show detected sign if any
                String detectedSign = handSignController.getDetectedSign();
                if (detectedSign != null) {
                    game.font.draw(game.batch, "Detected: " + detectedSign,
                        camX, camY + camHeight + 45);
                }
            }
        }

        // Draw timer overlay
        for (Sprite sprite : spritesForAttackSeq) {
            sprite.draw(game.batch);
        }
    }

    private void input() {
        float delta = Gdx.graphics.getDeltaTime();

        // Toggle control methods with H key
        if (Gdx.input.isKeyJustPressed(Input.Keys.H)) {
            useHeadControl = !useHeadControl;
            if (useHeadControl && headController.isHeadTrackingEnabled()) {
                System.out.println("Head control ENABLED (Absolute Positioning)");
            } else {
                System.out.println("Keyboard control ENABLED");
            }
        }

        // Toggle camera feed with C key
        if (Gdx.input.isKeyJustPressed(Input.Keys.C)) {
            headController.toggleCameraFeed();
        }

        // Only update head position if NOT in attack sequence
        if (useHeadControl && headController.isHeadTrackingEnabled() && !attackSeq) { // REMOVED: && !isGestureModeActive
            // ABSOLUTE POSITIONING: Head position directly controls character position
            headController.updateHeadPosition();

            // Get absolute positions from head controller
            float targetX = headController.getAbsoluteX();
            float targetY = headController.getAbsoluteY();

            // Smooth movement (optional - remove for instant teleport)
            float currentX = playerSprite.getX();
            float currentY = playerSprite.getY();
            float lerpFactor = 0.3f; // Adjust for smoothing (0 = instant, 1 = very slow)

            playerSprite.setX(currentX + (targetX - currentX) * lerpFactor);
            playerSprite.setY(currentY + (targetY - currentY) * lerpFactor);

            // For instant teleport (no smoothing), use:
            // playerSprite.setX(targetX);
            // playerSprite.setY(targetY);

        } else if (!attackSeq) { // REMOVED: && !isGestureModeActive
            // Original keyboard control (fallback)
            float speed = 8f;
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
            float cameraWidth = 16f;
            float cameraHeight = 16f; //make it fit in black square
            float cameraX = worldWidth - cameraWidth - 2f;  //make it bottom right
            float cameraY = 2f;

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

    public void pauseOrResumeGameForAttack() {
        attackSeq = !attackSeq;

        if (attackSeq) {
            // 1. STOP Head Tracking (Release Camera)
            if (headController != null) {
                headController.pauseCamera();
            }
            // 2. START Hand Tracking (Python Grabs Camera)
            if (handSignController != null) {
                handSignController.startSpellCasting(tempAttackDirection);
            }
        } else {
            // 1. STOP Hand Tracking (Python Releases Camera)
            if (handSignController != null) {
                handSignController.stopSpellCasting();
            }
            // 2. RESUME Head Tracking (Java Grabs Camera)
            if (headController != null) {
                headController.resumeCamera();
            }
        }
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

        if (handSignController != null) {
            handSignController.dispose();
        }
    }
}
