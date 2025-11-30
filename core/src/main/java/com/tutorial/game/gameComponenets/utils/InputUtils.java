package com.tutorial.game.gameComponenets.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * Utility for converting mouse/touch screen coordinates
 * into world coordinates when using a FitViewport.
 */
public class InputUtils {

    /**
     * Returns the current mouse position in world coordinates,
     * correctly accounting for FitViewport letterboxing.
     *
     * @param viewport the active viewport (e.g. your FitViewport)
     * @return Vector2 containing world coordinates
     */
    public static Vector2 getMouseWorldCoords(Viewport viewport) {
        // Raw screen coordinates
        int screenX = Gdx.input.getX();
        int screenY = Gdx.input.getY();

        // Viewport bounds (actual game area inside the window)
        int viewportX = viewport.getScreenX();
        int viewportY = viewport.getScreenY();
        int viewportWidth = viewport.getScreenWidth();
        int viewportHeight = viewport.getScreenHeight();

        // Adjust for letterbox offset
        int adjustedX = screenX - viewportX;
        int adjustedY = screenY - viewportY;

        // Unproject into world coordinates
        return viewport.unproject(new Vector2(adjustedX+viewport.getRightGutterWidth(), adjustedY+viewport.getBottomGutterHeight()));
    }
}
