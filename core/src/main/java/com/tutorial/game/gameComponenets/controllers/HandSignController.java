package com.tutorial.game.gameComponenets.controllers;

import org.opencv.core.*;
import org.opencv.imgproc.Moments;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import org.opencv.imgproc.Imgproc;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.List;

public class HandSignController {
    private VideoCapture camera;
    private Mat currentFrame;
    private Mat hsvFrame;
    private Mat skinMask;
    private Mat drawingFrame;
    private Texture cameraTexture;

    private boolean isActive;
    private float timeRemaining;
    private final long SPELL_TIME_LIMIT = 5000; // 5 seconds

    private String detectedSign;
    private long lastSignDetectionTime;
    private final long SIGN_COOLDOWN = 1000; // 1 second between detections

    // Hand sign types
    public enum HandSign {
        PEACE("up", "✌️"),
        FIST("down", "✊"),
        THUMBS_UP("right", "👍"),
        OPEN_HAND("left", "👋"),
        UNKNOWN(null, "❓");

        private final String attackDirection;
        private final String emoji;

        HandSign(String attackDirection, String emoji) {
            this.attackDirection = attackDirection;
            this.emoji = emoji;
        }

        public String getAttackDirection() {
            return attackDirection;
        }

        public String getEmoji() {
            return emoji;
        }
    }

    public HandSignController() {
        this.isActive = false;
        this.detectedSign = null;
        initializeMats();
    }

    private void initializeMats() {
        currentFrame = new Mat();
        hsvFrame = new Mat();
        skinMask = new Mat();
        drawingFrame = new Mat();
    }

    public void startSpellCasting(String requiredDirection) {
        openCamera();
        isActive = true;
        timeRemaining = SPELL_TIME_LIMIT / 1000f;
        detectedSign = null;
        lastSignDetectionTime = 0;
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

            // Set resolution
            camera.set(Videoio.CAP_PROP_FRAME_WIDTH, 320);
            camera.set(Videoio.CAP_PROP_FRAME_HEIGHT, 240);
            camera.set(Videoio.CAP_PROP_FPS, 15);

            System.out.println("✅ Hand sign camera opened");

        } catch (Exception e) {
            System.err.println("Error opening camera: " + e.getMessage());
        }
    }

    private void closeCamera() {
        if (camera != null && camera.isOpened()) {
            camera.release();
            camera = null;
            System.out.println("✅ Hand sign camera released");
        }
    }

    public void update() {
        if (!isActive || camera == null || !camera.isOpened()) return;

        // Update time
        timeRemaining -= 1/60f; // Assuming 60 FPS

        if (timeRemaining <= 0) {
            stopSpellCasting();
            return;
        }

        try {
            if (camera.read(currentFrame) && !currentFrame.empty()) {
                // Detect hand sign
                detectHandSign();

                // Update display
                updateCameraTexture();
            }
        } catch (Exception e) {
            System.err.println("Error updating hand sign camera: " + e.getMessage());
        }
    }

    private void detectHandSign() {
        // Flip for mirror view
        Core.flip(currentFrame, currentFrame, 1);

        // Create a copy for drawing
        currentFrame.copyTo(drawingFrame);

        // Convert to HSV for skin detection
        Imgproc.cvtColor(currentFrame, hsvFrame, Imgproc.COLOR_BGR2HSV);

        // Define skin color range in HSV
        Scalar lowerSkin = new Scalar(0, 30, 60);
        Scalar upperSkin = new Scalar(20, 150, 255);

        // Create skin mask
        Core.inRange(hsvFrame, lowerSkin, upperSkin, skinMask);

        // Apply morphological operations to clean up mask
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5));
        Imgproc.morphologyEx(skinMask, skinMask, Imgproc.MORPH_CLOSE, kernel);
        Imgproc.morphologyEx(skinMask, skinMask, Imgproc.MORPH_OPEN, kernel);

        // Find contours in the skin mask
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(skinMask, contours, hierarchy,
            Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        if (!contours.isEmpty()) {
            // Find the largest contour (hand)
            double maxArea = 0;
            MatOfPoint handContour = null;

            for (MatOfPoint contour : contours) {
                double area = Imgproc.contourArea(contour);
                if (area > maxArea) {
                    maxArea = area;
                    handContour = contour;
                }
            }

            if (handContour != null && maxArea > 1000) { // Minimum hand size
                // Analyze hand contour for sign detection
                analyzeHandContour(handContour);

                // Draw hand contour
                Imgproc.drawContours(drawingFrame, contours, contours.indexOf(handContour),
                    new Scalar(0, 255, 0), 2);
            }
        }

        // Draw detection info
        String status = detectedSign != null ?
            "Detected: " + detectedSign :
            "Show hand sign!";

        Imgproc.putText(drawingFrame, status, new Point(10, 30),
            Imgproc.FONT_HERSHEY_SIMPLEX, 0.7, new Scalar(0, 255, 255), 2);

        kernel.release();
        hierarchy.release();
    }

    private void analyzeHandContour(MatOfPoint contour) {
        // Simplify contour
        MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
        MatOfPoint2f approx2f = new MatOfPoint2f();

        double epsilon = 0.02 * Imgproc.arcLength(contour2f, true);
        Imgproc.approxPolyDP(contour2f, approx2f, epsilon, true);

        // Get convex hull and defects for finger detection
        MatOfInt hull = new MatOfInt();
        Imgproc.convexHull(new MatOfPoint(approx2f.toArray()), hull, false);

        MatOfInt4 defects = new MatOfInt4();
        if (hull.toArray().length > 3) {
            MatOfPoint points = new MatOfPoint(approx2f.toArray());
            Imgproc.convexityDefects(points, hull, defects);
        }

        // Count fingers based on convexity defects
        int fingerCount = countFingers(defects, contour2f);

        // Detect hand sign based on finger count and shape
        HandSign sign = detectSignFromFingerCount(fingerCount, contour2f);

        // Update detected sign with cooldown
        long currentTime = System.currentTimeMillis();
        if (sign != HandSign.UNKNOWN && currentTime - lastSignDetectionTime > SIGN_COOLDOWN) {
            detectedSign = sign.getEmoji() + " " + sign.getAttackDirection().toUpperCase();
            lastSignDetectionTime = currentTime;
            System.out.println("Detected hand sign: " + sign + " → " + sign.getAttackDirection());
        }

        // Draw finger count
        Point center = getContourCenter(contour);
        Imgproc.putText(drawingFrame, "Fingers: " + fingerCount,
            new Point(center.x - 30, center.y - 20),
            Imgproc.FONT_HERSHEY_SIMPLEX, 0.6, new Scalar(255, 0, 0), 2);

        contour2f.release();
        approx2f.release();
        hull.release();
        defects.release();
    }

    private int countFingers(MatOfInt4 defects, MatOfPoint2f contour) {
        if (defects.empty()) return 0;

        int fingerCount = 0;
        Point[] contourPoints = contour.toArray();
        int[] defectsArray = defects.toArray();

        for (int i = 0; i < defectsArray.length; i += 4) {
            int startIdx = defectsArray[i];
            int endIdx = defectsArray[i + 1];
            int farIdx = defectsArray[i + 2];
            float depth = Float.intBitsToFloat(defectsArray[i + 3]) / 256;

            // A defect likely indicates a finger if depth is significant
            if (depth > 10) { // Threshold for finger
                fingerCount++;
            }
        }

        // Typically, defects = fingers - 1
        return Math.min(fingerCount + 1, 5);
    }

    private HandSign detectSignFromFingerCount(int fingerCount, MatOfPoint2f contour) {
        switch (fingerCount) {
            case 0:
            case 1:
                return HandSign.FIST; // Closed hand or thumb only
            case 2:
                return HandSign.PEACE; // Peace sign
            case 5:
                return HandSign.OPEN_HAND; // Open hand
            default:
                // Check if it's thumbs up (1 finger but specific shape)
                Rect boundingRect = Imgproc.boundingRect(new MatOfPoint(contour.toArray()));
                double aspectRatio = (double) boundingRect.width / boundingRect.height;

                if (aspectRatio < 0.8) {
                    return HandSign.THUMBS_UP; // Tall and narrow
                }
                return HandSign.UNKNOWN;
        }
    }

    private Point getContourCenter(MatOfPoint contour) {
        Moments moments = Imgproc.moments(contour);
        double m00 = moments.m00;
        if (m00 > 0) {
            return new Point(moments.m10 / m00, moments.m01 / m00);
        }
        return new Point(0, 0);
    }

    private void updateCameraTexture() {
        if (drawingFrame.empty()) return;

        try {
            // Draw timer
            String timerText = String.format("TIME: %.1fs", timeRemaining);
            Imgproc.putText(drawingFrame, timerText, new Point(10, 60),
                Imgproc.FONT_HERSHEY_SIMPLEX, 0.7, new Scalar(255, 255, 0), 2);

            // Convert to texture
            BufferedImage image = matToBufferedImage(drawingFrame);

            if (cameraTexture != null) {
                cameraTexture.dispose();
            }
            cameraTexture = bufferedImageToTexture(image);

        } catch (Exception e) {
            System.err.println("Error updating camera texture: " + e.getMessage());
        }
    }

    private BufferedImage matToBufferedImage(Mat mat) {
        int type = BufferedImage.TYPE_3BYTE_BGR;
        byte[] buffer = new byte[mat.cols() * mat.rows() * mat.channels()];
        mat.get(0, 0, buffer);

        BufferedImage image = new BufferedImage(mat.cols(), mat.rows(), type);
        System.arraycopy(buffer, 0,
            ((DataBufferByte) image.getRaster().getDataBuffer()).getData(),
            0, buffer.length);
        return image;
    }

    private Texture bufferedImageToTexture(BufferedImage image) {
        Pixmap pixmap = new Pixmap(image.getWidth(), image.getHeight(), Pixmap.Format.RGB888);

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                pixmap.setColor(r / 255f, g / 255f, b / 255f, 1f);
                pixmap.drawPixel(x, y);
            }
        }

        return new Texture(pixmap);
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
        if (detectedSign == null) return null;

        // Extract direction from detected sign string
        if (detectedSign.contains("up")) return "up";
        if (detectedSign.contains("down")) return "down";
        if (detectedSign.contains("left")) return "left";
        if (detectedSign.contains("right")) return "right";
        return null;
    }

    public String getDetectedSign() {
        return detectedSign;
    }

    public boolean hasDetectedDirection() {
        return getDetectedDirection() != null;
    }

    public float getTimeRemaining() {
        return timeRemaining;
    }


    public void dispose() {
        closeCamera();

        if (cameraTexture != null) {
            cameraTexture.dispose();
            cameraTexture = null;
        }
        if (currentFrame != null) {
            currentFrame.release();
        }
        if (hsvFrame != null) {
            hsvFrame.release();
        }
        if (skinMask != null) {
            skinMask.release();
        }
        if (drawingFrame != null) {
            drawingFrame.release();
        }
    }
}
