package com.tutorial.game.gameComponenets.controllers;

import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.task.vision.detector.Detection;
import org.tensorflow.lite.task.vision.detector.ObjectDetector;
import org.tensorflow.lite.task.core.BaseOptions;
import org.tensorflow.lite.task.vision.core.BaseVisionTaskApi;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.Rot90Op;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;
import org.tensorflow.lite.support.image.ops.NormalizeOp;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.Gdx;
import org.opencv.core.*;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

public class TFLiteHandSignController {
    private VideoCapture camera;
    private Mat currentFrame;
    private Texture cameraTexture;
    private ObjectDetector handDetector;

    private boolean isActive;
    private float timeRemaining;
    private final long SPELL_TIME_LIMIT = 5000;

    private String detectedDirection;
    private long lastDetectionTime;

    // Hand landmark indices (MediaPipe format)
    private static final int WRIST = 0;
    private static final int THUMB_CMC = 1;
    private static final int THUMB_MCP = 2;
    private static final int THUMB_IP = 3;
    private static final int THUMB_TIP = 4;
    private static final int INDEX_FINGER_MCP = 5;
    private static final int INDEX_FINGER_PIP = 6;
    private static final int INDEX_FINGER_DIP = 7;
    private static final int INDEX_FINGER_TIP = 8;
    private static final int MIDDLE_FINGER_MCP = 9;
    private static final int MIDDLE_FINGER_PIP = 10;
    private static final int MIDDLE_FINGER_DIP = 11;
    private static final int MIDDLE_FINGER_TIP = 12;
    private static final int RING_FINGER_MCP = 13;
    private static final int RING_FINGER_PIP = 14;
    private static final int RING_FINGER_DIP = 15;
    private static final int RING_FINGER_TIP = 16;
    private static final int PINKY_MCP = 17;
    private static final int PINKY_PIP = 18;
    private static final int PINKY_DIP = 19;
    private static final int PINKY_TIP = 20;

    public TFLiteHandSignController() {
        this.isActive = false;
        this.detectedDirection = null;
        this.currentFrame = new Mat();

        // Initialize TFLite model
        initializeModel();
    }

    private void initializeModel() {
        try {
            // Load model from assets
            InputStream modelStream = Gdx.files.internal("models/hand_landmarker.task").read();
            byte[] modelBytes = new byte[modelStream.available()];
            modelStream.read(modelBytes);
            modelStream.close();

            // Create ObjectDetector options
            ObjectDetector.ObjectDetectorOptions options =
                ObjectDetector.ObjectDetectorOptions.builder()
                    .setBaseOptions(BaseOptions.builder().build())
                    .setMaxResults(1)  // Detect only one hand
                    .setScoreThreshold(0.5f)
                    .build();

            // Create detector
            handDetector = ObjectDetector.createFromBufferAndOptions(
                ByteBuffer.wrap(modelBytes), options);

            System.out.println("✅ TFLite hand detector initialized");

        } catch (Exception e) {
            System.err.println("❌ Failed to initialize TFLite model: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void startSpellCasting(String requiredDirection) {
        openCamera();
        isActive = true;
        timeRemaining = SPELL_TIME_LIMIT / 1000f;
        detectedDirection = null;
        lastDetectionTime = 0;
    }

    public void stopSpellCasting() {
        isActive = false;
        closeCamera();
    }

    private void openCamera() {
        try {
            if (camera != null && camera.isOpened()) {
                camera.release();
            }

            camera = new VideoCapture(0);
            if (!camera.isOpened()) {
                System.err.println("❌ Hand sign camera not accessible");
                return;
            }

            camera.set(Videoio.CAP_PROP_FRAME_WIDTH, 320);
            camera.set(Videoio.CAP_PROP_FRAME_HEIGHT, 240);
            camera.set(Videoio.CAP_PROP_FPS, 15);

            System.out.println("✅ TFLite hand sign camera opened");

        } catch (Exception e) {
            System.err.println("Error opening camera: " + e.getMessage());
        }
    }

    private void closeCamera() {
        if (camera != null && camera.isOpened()) {
            camera.release();
            camera = null;
        }
    }

    public void update() {
        if (!isActive || camera == null || !camera.isOpened()) return;

        // Update time
        timeRemaining -= 1/60f;

        if (timeRemaining <= 0) {
            stopSpellCasting();
            return;
        }

        try {
            if (camera.read(currentFrame) && !currentFrame.empty()) {
                // Flip for mirror view
                Core.flip(currentFrame, currentFrame, 1);

                // Detect hand and analyze sign
                detectHandSign();

                // Update display texture
                updateCameraTexture();
            }
        } catch (Exception e) {
            System.err.println("Error in TFLite hand detection: " + e.getMessage());
        }
    }

    private void detectHandSign() {
        if (handDetector == null) return;

        try {
            // Convert OpenCV Mat to TensorImage
            Mat rgbFrame = new Mat();
            Imgproc.cvtColor(currentFrame, rgbFrame, Imgproc.COLOR_BGR2RGB);

            // Create TensorImage from Mat
            TensorImage tensorImage = new TensorImage(DataType.UINT8);
            tensorImage.load(rgbFrame);

            // Detect hand
            List<Detection> detections = handDetector.detect(tensorImage);

            if (!detections.isEmpty()) {
                Detection hand = detections.get(0);

                // Get bounding box
                RectF boundingBox = hand.getBoundingBox();

                // Draw bounding box
                int x = (int)(boundingBox.left * currentFrame.width());
                int y = (int)(boundingBox.top * currentFrame.height());
                int width = (int)(boundingBox.width() * currentFrame.width());
                int height = (int)(boundingBox.height() * currentFrame.height());

                Imgproc.rectangle(currentFrame,
                    new Point(x, y),
                    new Point(x + width, y + height),
                    new Scalar(0, 255, 0), 2);

                // For now, use a simpler approach since we don't have landmarks
                // In a full implementation, we'd use a hand landmark model
                detectSignFromBoundingBox(boundingBox);
            }

            rgbFrame.release();

        } catch (Exception e) {
            System.err.println("Error in hand detection: " + e.getMessage());
        }
    }

    private void detectSignFromBoundingBox(RectF boundingBox) {
        float aspectRatio = boundingBox.width() / boundingBox.height();

        // Simple sign detection based on bounding box shape
        // This is temporary - we should use actual hand landmarks
        if (aspectRatio > 1.2) {
            detectedDirection = "right"; // Wide = open hand or peace sideways
        } else if (aspectRatio < 0.8) {
            detectedDirection = "up"; // Tall = peace sign or thumbs up
        } else {
            // Roughly square
            if (boundingBox.height() > 0.4) { // Large = open hand
                detectedDirection = "left";
            } else { // Small = fist
                detectedDirection = "down";
            }
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastDetectionTime > 500) { // 0.5s cooldown
            System.out.println("Detected hand sign: " + detectedDirection +
                " (aspect: " + aspectRatio + ")");
            lastDetectionTime = currentTime;
        }
    }

    private void updateCameraTexture() {
        if (currentFrame.empty()) return;

        try {
            // Draw info on frame
            String status = detectedDirection != null ?
                "Detected: " + detectedDirection.toUpperCase() :
                "Show hand sign!";

            Imgproc.putText(currentFrame, status, new Point(10, 30),
                Imgproc.FONT_HERSHEY_SIMPLEX, 0.7, new Scalar(0, 255, 255), 2);

            // Draw timer
            String timerText = String.format("TIME: %.1fs", timeRemaining);
            Imgproc.putText(currentFrame, timerText, new Point(10, 60),
                Imgproc.FONT_HERSHEY_SIMPLEX, 0.7, new Scalar(255, 255, 0), 2);

            // Convert to texture
            byte[] buffer = new byte[currentFrame.cols() * currentFrame.rows() * 3];
            currentFrame.get(0, 0, buffer);

            Pixmap pixmap = new Pixmap(currentFrame.cols(), currentFrame.rows(),
                Pixmap.Format.RGB888);

            for (int y = 0; y < currentFrame.rows(); y++) {
                for (int x = 0; x < currentFrame.cols(); x++) {
                    int index = (y * currentFrame.cols() + x) * 3;
                    int b = buffer[index] & 0xFF;
                    int g = buffer[index + 1] & 0xFF;
                    int r = buffer[index + 2] & 0xFF;

                    pixmap.setColor(r / 255f, g / 255f, b / 255f, 1f);
                    pixmap.drawPixel(x, y);
                }
            }

            if (cameraTexture != null) {
                cameraTexture.dispose();
            }
            cameraTexture = new Texture(pixmap);
            pixmap.dispose();

        } catch (Exception e) {
            System.err.println("Error updating texture: " + e.getMessage());
        }
    }

    public void draw(SpriteBatch batch, float x, float y, float width, float height) {
        if (cameraTexture != null && isActive) {
            batch.draw(cameraTexture, x, y, width, height);
        }
    }

    public boolean isActive() {
        return isActive;
    }

    public String getDetectedDirection() {
        return detectedDirection;
    }

    public boolean hasDetectedDirection() {
        return detectedDirection != null;
    }

    public float getTimeRemaining() {
        return timeRemaining;
    }

    public String getDetectedSign() {
        return detectedDirection;
    }

    public void dispose() {
        closeCamera();

        if (cameraTexture != null) {
            cameraTexture.dispose();
            cameraTexture = null;
        }

        if (handDetector != null) {
            handDetector.close();
            handDetector = null;
        }

        if (currentFrame != null) {
            currentFrame.release();
        }
    }
}
