package com.tutorial.game.gameComponenets.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.utils.ScreenUtils;
import com.tutorial.game.MainGame;

public class StartScreen implements Screen {
    final MainGame game;
    Sprite playerSprite;
    Texture screen;

    public StartScreen(final MainGame game) {
       this.game = game;
       screen = new Texture("startscreen.png");
       playerSprite = new Sprite(new Texture("player.png"));
       playerSprite.setSize(16,16);
       playerSprite.setOrigin(playerSprite.getWidth()/2,playerSprite.getHeight()/2);
       playerSprite.setPosition(37.5f,3);
       playerSprite.setRotation(0);
    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(Color.BLACK);

        game.viewport.apply();
        game.batch.setProjectionMatrix(game.viewport.getCamera().combined);

        game.batch.begin();
        game.batch.draw(screen,0,0, game.viewport.getWorldWidth(),game.viewport.getWorldHeight());
        //draw text. Remember that x and y are in meters
        playerSprite.draw(game.batch);
        game.batch.end();

        if (Gdx.input.isTouched()) {
            game.setScreen(new GameScreen(game));
            dispose();
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
