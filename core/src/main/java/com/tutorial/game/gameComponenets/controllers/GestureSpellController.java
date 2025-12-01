package com.tutorial.game.gameComponenets.controllers;

import org.opencv.core.*;
import org.opencv.imgproc.Moments;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import org.opencv.imgproc.Imgproc;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.List;

public class GestureSpellController {
    private VideoCapture camera;
    private Mat currentFrame;
    private Mat grayFrame;
    private Mat drawingCanvas;
    private Texture cameraTexture;

    private List<Point> drawnPoints;
    private boolean isDrawing;
    private Point lastPoint;
    private long drawingStartTime;
    private final long DRAWING_TIME_LIMIT = 30000; //TIME TO DRAW

    private boolean isActive;
    private float timeRemaining;

    // Shape detection
    private String detectedDirection;

    public GestureSpellController() {
        this.isActive = false;
        this.drawnPoints = new ArrayList<>();
        this.isDrawing = false;
        this.detectedDirection = null;

        // Initialize frames (camera will be opened when needed)
        currentFrame = new Mat();
        grayFrame = new Mat();
        drawingCanvas = new Mat(240, 320, CvType.CV_8UC3, new Scalar(0, 0, 0));
    }

    public void startSpellCasting(String requiredDirection) {
        // Open camera for gesture mode
        openCamera();

        isActive = true;
        isDrawing = false;
        drawnPoints.clear();
        drawingCanvas.setTo(new Scalar(0, 0, 0));
        drawingStartTime = System.currentTimeMillis();
        timeRemaining = DRAWING_TIME_LIMIT / 1000f;
        detectedDirection = null;
    }

    public void stopSpellCasting() {
        isActive = false;
        isDrawing = false;

        // Analyze shape when stopping
        if (!drawnPoints.isEmpty()) {
            analyzeShape();
        }

        // Release camera so head tracking can use it
        closeCamera();
    }

    private void openCamera() {
        try {
            // Close if already open
            if (camera != null && camera.isOpened()) {
                camera.release();
            }

            // Open camera
            camera = new VideoCapture(0);
            if (!camera.isOpened()) {
                System.err.println("❌ Gesture camera not accessible");
                return;
            }

            // Set low resolution for performance
            camera.set(Videoio.CAP_PROP_FRAME_WIDTH, 320);
            camera.set(Videoio.CAP_PROP_FRAME_HEIGHT, 240);
            camera.set(Videoio.CAP_PROP_FPS, 15);

            System.out.println("✅ Gesture camera opened");

        } catch (Exception e) {
            System.err.println("Error opening gesture camera: " + e.getMessage());
        }
    }

    private void closeCamera() {
        if (camera != null && camera.isOpened()) {
            camera.release();
            camera = null;
            System.out.println("✅ Gesture camera released");
        }
    }

    public void update() {
        if (!isActive || camera == null || !camera.isOpened()) return;

        // Update time remaining
        long elapsed = System.currentTimeMillis() - drawingStartTime;
        timeRemaining = Math.max(0, (DRAWING_TIME_LIMIT - elapsed) / 1000f);

        // Time's up - stop and analyze
        if (timeRemaining <= 0) {
            stopSpellCasting();
            return;
        }

        try {
            // Read frame
            if (camera.read(currentFrame) && !currentFrame.empty()) {
                // Simple hand detection based on brightness
                detectHand();

                // Update display texture
                updateCameraTexture();
            }
        } catch (Exception e) {
            System.err.println("Error updating gesture camera: " + e.getMessage());
        }
    }

    private void detectHand() {
        // Convert to grayscale
        Imgproc.cvtColor(currentFrame, grayFrame, Imgproc.COLOR_BGR2GRAY);

        // Simple threshold to detect bright hand (assume hand is well-lit)
        Mat threshold = new Mat();
        Imgproc.threshold(grayFrame, threshold, 200, 255, Imgproc.THRESH_BINARY);

        // Find contours
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(threshold, contours, hierarchy,
            Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        Point handPoint = null;

        if (!contours.isEmpty()) {
            // Find the largest contour (likely the hand)
            double maxArea = 0;
            MatOfPoint largestContour = null;

            for (MatOfPoint contour : contours) {
                double area = Imgproc.contourArea(contour);
                if (area > maxArea) {
                    maxArea = area;
                    largestContour = contour;
                }
            }

            if (largestContour != null && maxArea > 500) { // Minimum area threshold
                // Get the centroid of the largest contour
                Moments moments = Imgproc.moments(largestContour);
                if (moments.get_m00() > 0) {
                    double centerX = moments.get_m10() / moments.get_m00();
                    double centerY = moments.get_m01() / moments.get_m00();
                    handPoint = new Point(centerX, centerY);

                    // Draw contour on frame for visualization
                    Imgproc.drawContours(currentFrame, contours, contours.indexOf(largestContour),
                        new Scalar(0, 255, 0), 2);
                }
            }
        }

        // Fallback: if no contours found, use the brightest point
        if (handPoint == null) {
            Core.MinMaxLocResult mmr = Core.minMaxLoc(threshold);
            if (mmr.maxVal > 200) {
                handPoint = mmr.maxLoc;
            }
        }

        if (handPoint != null) {
            // Draw cursor on frame
            Imgproc.circle(currentFrame, handPoint, 10, new Scalar(255, 0, 0), 2);

            // If drawing is active, add to path
            if (isDrawing) {
                drawnPoints.add(handPoint);

                // Draw on canvas
                if (lastPoint != null) {
                    Imgproc.line(drawingCanvas, lastPoint, handPoint,
                        new Scalar(0, 0, 255), 3);
                }

                lastPoint = handPoint;
            }
        }

        threshold.release();
        hierarchy.release();
    }

    public void startDrawing() {
        if (isActive) {
            isDrawing = true;
            if (drawnPoints.isEmpty()) {
                drawnPoints.clear();
                lastPoint = null;
            }
        }
    }

    public void stopDrawing() {
        isDrawing = false;
        if (!drawnPoints.isEmpty()) {
            analyzeShape();
        }
    }

    private void analyzeShape() {
        if (drawnPoints.size() < 5) {
            detectedDirection = null;
            System.out.println("✗ Not enough points drawn: " + drawnPoints.size());
            return;
        }

        // Simple shape analysis
        double minX = Double.MAX_VALUE, maxX = Double.MIN_VALUE;
        double minY = Double.MAX_VALUE, maxY = Double.MIN_VALUE;

        for (Point p : drawnPoints) {
            minX = Math.min(minX, p.x);
            maxX = Math.max(maxX, p.x);
            minY = Math.min(minY, p.y);
            maxY = Math.max(maxY, p.y);
        }

        double width = maxX - minX;
        double height = maxY - minY;
        double aspectRatio = width / height;

        System.out.println("Shape analysis - Width: " + width + ", Height: " + height +
            ", Aspect: " + aspectRatio + ", Points: " + drawnPoints.size());

        // Simple shape detection
        if (aspectRatio > 1.5 && width > 50) {
            detectedDirection = "right"; // Horizontal line
            System.out.println("→ Detected: RIGHT (horizontal line)");
        } else if (aspectRatio < 0.66 && height > 50) {
            detectedDirection = "up"; // Vertical line
            System.out.println("↑ Detected: UP (vertical line)");
        } else if (width > 30 && height > 30) {
            // Roughly square area
            if (height > width * 1.3) {
                detectedDirection = "down"; // Tall shape (triangle-ish)
                System.out.println("↓ Detected: DOWN (tall shape)");
            } else {
                detectedDirection = "left"; // Square-ish
                System.out.println("← Detected: LEFT (square shape)");
            }
        } else {
            detectedDirection = null;
            System.out.println("? Could not detect shape");
        }
    }

    private void updateCameraTexture() {
        if (currentFrame.empty()) return;

        try {
            // Combine frames for display
            Mat displayFrame = new Mat();
            Core.addWeighted(currentFrame, 0.7, drawingCanvas, 0.3, 0, displayFrame);

            // Draw timer
            String timerText = String.format("TIME: %.1fs", timeRemaining);
            Imgproc.putText(displayFrame, timerText, new Point(10, 30),
                Imgproc.FONT_HERSHEY_SIMPLEX, 0.7, new Scalar(0, 255, 255), 2);

            // Draw instructions
            if (isDrawing) {
                Imgproc.putText(displayFrame, "DRAWING...", new Point(10, 60),
                    Imgproc.FONT_HERSHEY_SIMPLEX, 0.6, new Scalar(255, 0, 0), 2);
            } else {
                Imgproc.putText(displayFrame, "HOLD LEFT CLICK TO DRAW", new Point(10, 60),
                    Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(255, 255, 0), 1);
            }

            // Convert to texture
            BufferedImage image = matToBufferedImage(displayFrame);
            displayFrame.release();

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
        return detectedDirection;
    }

    public boolean hasDetectedDirection() {
        return detectedDirection != null;
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
            currentFrame = null;
        }
        if (grayFrame != null) {
            grayFrame.release();
            grayFrame = null;
        }
        if (drawingCanvas != null) {
            drawingCanvas.release();
            drawingCanvas = null;
        }
    }
}
