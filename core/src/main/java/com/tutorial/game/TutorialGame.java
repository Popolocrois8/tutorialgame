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

import com.badlogic.gdx.files.FileHandle;

import java.nio.ByteBuffer;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class TutorialGame extends ApplicationAdapter {

    private SpriteBatch batch;
    private Texture image;
    private Texture cameraTexture;

    private PythonHandTracker handTracker;
    private VideoCapture camera;
    private boolean handTrackingActive = false;

    //for mode switching
    private boolean faceDetectionMode = true; // true = FaceDetector, false = HandTracker
    private String currentMode = "Face Detection";

    private FaceDetector faceDetector;

    @Override
    public void create() {
        batch = new SpriteBatch();
        image = new Texture("libgdx.png");

        // Initialize OpenCV
        nu.pattern.OpenCV.loadLocally();

        // Initialize FaceDetector
        FileHandle cascadeFile = Gdx.files.internal("haarcascade_frontalface_alt.xml");
        faceDetector = new FaceDetector(cascadeFile.file().getAbsolutePath());

        // Initialize hand tracker
        handTracker = new PythonHandTracker();

        // Try to connect to Python server
        new Thread(() -> {
            try {
                Thread.sleep(5000); // Wait for Python server
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

        // Check for mode switch (Space key)
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.SPACE)) {
            faceDetectionMode = !faceDetectionMode;
            currentMode = faceDetectionMode ? "Face Detection" : "Hand Tracking";
            System.out.println("Switched to: " + currentMode);
        }

        Mat processedFrame = null;

        if (faceDetectionMode) {
            // Use FaceDetector (projectile dodging)
            processedFrame = faceDetector.processFrame();
        } else if (handTrackingActive) {
            // Use HandTracker (spell drawing)
            // Create a blank frame with the same size as FaceDetector's frame
            Mat testFrame = getCameraFrame();
            if (testFrame != null) {
                processedFrame = handTracker.processFrame(testFrame);
                testFrame.release();
            }
        }

        if (processedFrame != null) {
            cameraTexture = matToTexture(processedFrame);
            processedFrame.release();
        }

        batch.begin();

        // Draw camera feed if available
        if (cameraTexture != null) {
            batch.draw(cameraTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

            // Draw current mode info
            // You'll need a BitmapFont for this, but for now we'll use System.out
        } else {
            // Draw fallback image
            batch.draw(image, 140, 210);
        }

        batch.end();
    }

    private Mat getCameraFrame() {
        // Use FaceDetector's camera to avoid conflicts
        return faceDetector.getRawCameraFrame();
    }


    private Texture matToTexture(Mat mat) {
        if (mat.empty()) return null;

        try {
            // Convert BGR to RGB
            Mat rgbMat = new Mat();
            Imgproc.cvtColor(mat, rgbMat, Imgproc.COLOR_BGR2RGB);

            // Create texture from Mat data
            byte[] data = new byte[rgbMat.cols() * rgbMat.rows() * (int)rgbMat.elemSize()];
            rgbMat.get(0, 0, data);

            // Create Pixmap and Texture directly
            com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(
                rgbMat.cols(), rgbMat.rows(), com.badlogic.gdx.graphics.Pixmap.Format.RGB888
            );

            // Copy data to Pixmap
            com.badlogic.gdx.utils.BufferUtils.copy(data, 0, pixmap.getPixels(), data.length);

            Texture texture = new Texture(pixmap);
            pixmap.dispose(); // Important: dispose the pixmap after creating texture

            rgbMat.release();
            return texture;

        } catch (Exception e) {
            System.err.println("Error converting Mat to Texture: " + e.getMessage());
            return null;
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        image.dispose();
        if (cameraTexture != null) {
            cameraTexture.dispose();
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
