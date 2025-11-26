package com.tutorial.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import java.nio.ByteBuffer;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class TutorialGame extends ApplicationAdapter {
    private SpriteBatch batch;
    private Texture image;
    private Texture cameraTexture;

    private PythonHandTracker handTracker;
    private VideoCapture camera;
    private boolean handTrackingActive = false;

    @Override
    public void create() {
        batch = new SpriteBatch();
        image = new Texture("libgdx.png");

        // Initialize OpenCV
        nu.pattern.OpenCV.loadLocally();

        // Initialize camera (Maybe camera are initialized twice at the same time??)
        camera = new VideoCapture(0);

        // Initialize hand tracker
        handTracker = new PythonHandTracker();

        // Try to connect to Python server
        new Thread(() -> {
            try {
                Thread.sleep(2000); // Wait a bit for Python server to start
                boolean connected = handTracker.connect("localhost", 12345);
                if (connected) {
                    handTrackingActive = true;
                    System.out.println("✅ Hand tracking connected!");
                } else {
                    System.out.println("❌ Failed to connect to hand tracker");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void render() {
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);

        // Process camera frame if hand tracking is active
        if (handTrackingActive && camera.isOpened()) {
            Mat frame = new Mat();
            camera.read(frame);

            if (!frame.empty()) {
                // Process frame with hand tracker
                Mat processedFrame = handTracker.processFrame(frame);

                // Convert OpenCV Mat to libGDX Texture
                cameraTexture = matToTexture(processedFrame);

                frame.release();
                if (processedFrame != frame) { // Only release if it's a different object
                    processedFrame.release();
                }
            }
        }

        batch.begin();

        // Draw camera feed if available
        if (cameraTexture != null) {
            batch.draw(cameraTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        } else {
            // Draw fallback image
            batch.draw(image, 140, 210);

            // Draw status message
            // (You'd need a BitmapFont for text, but for now we'll use the image)
        }

        batch.end();
    }

    private Texture matToTexture(Mat mat) {
        if (mat.empty()) return null;

        // Convert BGR to RGB
        Mat rgbMat = new Mat();
        Imgproc.cvtColor(mat, rgbMat, Imgproc.COLOR_BGR2RGB);

        // Create texture from Mat data
        byte[] data = new byte[rgbMat.cols() * rgbMat.rows() * (int)rgbMat.elemSize()];
        rgbMat.get(0, 0, data);

        Texture texture = new Texture(rgbMat.cols(), rgbMat.rows(), com.badlogic.gdx.graphics.Pixmap.Format.RGB888);
        texture.getTextureData().prepare();
        ByteBuffer buffer = texture.getTextureData().consumePixmap().getPixels();
        buffer.put(data);
        buffer.rewind();

        rgbMat.release();
        return texture;
    }

    @Override
    public void dispose() {
        batch.dispose();
        image.dispose();
        if (cameraTexture != null) {
            cameraTexture.dispose();
        }
        if (camera != null && camera.isOpened()) {
            camera.release();
        }
        if (handTracker != null) {
            handTracker.disconnect();
        }
    }

    @Override
    public void resize(int width, int height) {
        batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
    }
}
