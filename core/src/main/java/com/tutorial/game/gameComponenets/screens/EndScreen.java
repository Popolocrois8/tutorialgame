package com.tutorial.game.gameComponenets.screens;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.ScreenUtils;
import com.tutorial.game.MainGame;

public class EndScreen implements Screen {
    final MainGame game;
    int score;
    String gameTimeString;

    public EndScreen(final MainGame game, float gameTime) {
        this.game = game;
        score = (int) (gameTime*100);
        gameTimeString = Integer.toString((int) Math.floor(gameTime/60))+"min "+Integer.toString((int) Math.floor(gameTime%60))+"s!";
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
        //draw text. Remember that x and y are in meters
        game.font.draw(game.batch, "GAME OVER!!! ", 1, 1.5f);
        game.font.draw(game.batch, "You played for "+gameTimeString, 2, 4.5f);
        game.font.draw(game.batch, "Your score: "+score, 2, 6.5f);
        game.batch.end();
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
