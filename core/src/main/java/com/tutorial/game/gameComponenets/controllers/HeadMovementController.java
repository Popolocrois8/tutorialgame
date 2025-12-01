package com.tutorial.game.gameComponenets.controllers;

import org.opencv.core.*;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import org.opencv.imgproc.Imgproc;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;

public class HeadMovementController {
    private VideoCapture camera;
    private CascadeClassifier faceDetector;
    private Mat currentFrame;
    private Mat rgbaFrame;
    private Point faceCenter;
    private boolean isInitialized;

    // For camera feed texture
    private Texture cameraTexture;
    private boolean showCameraFeed = false; // Disabled by default for performance

    // Game world boundaries (adjust these to match your game)
    private final float GAME_MIN_X = 6f;
    private final float GAME_MAX_X = 26f;
    private final float GAME_MIN_Y = 6f;
    private final float GAME_MAX_Y = 26f;

    // Performance optimization
    private long lastProcessTime = 0;
    private final long PROCESS_INTERVAL_MS = 100; // Process every 100ms (10 FPS)
    private int cameraWidth = 640;
    private int cameraHeight = 480;

    public HeadMovementController() {
        this.faceCenter = new Point(0, 0);
        this.isInitialized = false;
        initializeCamera();
    }

    private void initializeCamera() {
        try {
            // Load OpenCV DLL (use your existing working path)
            File dllFile = new File("libs\\opencv_java451.dll"); //use load instead of loadlibrary
            System.load(dllFile.getAbsolutePath());
            System.out.println("✅ Loaded OpenCV from: " + dllFile);

            // Initialize camera with low resolution for performance
            camera = new VideoCapture(0);
            if (!camera.isOpened()) {
                System.err.println("❌ Camera not accessible");
                return;
            }

            // Set low resolution for faster processing
            camera.set(Videoio.CAP_PROP_FRAME_WIDTH, 640);
            camera.set(Videoio.CAP_PROP_FRAME_HEIGHT, 480);
            camera.set(Videoio.CAP_PROP_FPS, 15);

            // Get actual camera dimensions
            cameraWidth = (int) camera.get(Videoio.CAP_PROP_FRAME_WIDTH);
            cameraHeight = (int) camera.get(Videoio.CAP_PROP_FRAME_HEIGHT);
            System.out.println("✅ Camera: " + cameraWidth + "x" + cameraHeight + " @ 15 FPS");

            // Initialize face detector
            faceDetector = new CascadeClassifier();
            boolean cascadeLoaded = faceDetector.load("haarcascade_frontalface_alt.xml");
            if (!cascadeLoaded) {
                System.err.println("❌ Could not load face detector");
                return;
            }

            // Initialize frames
            currentFrame = new Mat();
            rgbaFrame = new Mat();

            // Test camera
            if (camera.read(currentFrame) && !currentFrame.empty()) {
                isInitialized = true;
                // Set initial face position to center
                faceCenter = new Point(cameraWidth / 2.0, cameraHeight / 2.0);
                System.out.println("✅ Head Movement Controller initialized");
            }

        } catch (Exception e) {
            System.err.println("❌ Error initializing camera: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void updateHeadPosition() {
        if (!isInitialized || camera == null) return;

        // Performance: Limit processing rate
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastProcessTime < PROCESS_INTERVAL_MS) {
            return; // Skip this frame
        }
        lastProcessTime = currentTime;

        try {
            // Read frame from camera
            if (camera.read(currentFrame) && !currentFrame.empty()) {
                //do NOT CONVERT TO  RGB
                currentFrame.copyTo(rgbaFrame);

                // Detect face (simplified for performance)
                detectFaceSimple();

                // Update camera texture only if feed is enabled
                if (showCameraFeed) {
                    updateCameraTexture();
                }
            }
        } catch (Exception e) {
            System.err.println("Error updating head position: " + e.getMessage());
        }
    }

    private void detectFaceSimple() {
        MatOfRect faceDetections = new MatOfRect();
        Mat grayFrame = new Mat();

        // Use smaller frame for faster processing
        Mat smallFrame = new Mat();
        Imgproc.resize(currentFrame, smallFrame, new Size(320, 240));

        // Convert to grayscale
        Imgproc.cvtColor(smallFrame, grayFrame, Imgproc.COLOR_BGR2GRAY);
        Imgproc.equalizeHist(grayFrame, grayFrame);

        // Fast face detection with minimal accuracy
        faceDetector.detectMultiScale(grayFrame, faceDetections, 1.1, 2, 0,
            new Size(50, 50), new Size(200, 200));

        Rect[] facesArray = faceDetections.toArray();
        if (facesArray.length > 0) {
            // Use the first detected face
            Rect face = facesArray[0];

            // Scale coordinates back to original camera size
            double scaleX = (double) cameraWidth / smallFrame.width();
            double scaleY = (double) cameraHeight / smallFrame.height();

            // Calculate face center in camera coordinates
            faceCenter = new Point(
                (face.x + face.width / 2.0) * scaleX,
                (face.y + face.height / 2.0) * scaleY
            );

            // Draw overlay only if camera feed is enabled
            if (showCameraFeed) {
                // Scale rectangle coordinates
                double rectX = face.x * scaleX;
                double rectY = face.y * scaleY;
                double rectWidth = face.width * scaleX;
                double rectHeight = face.height * scaleY;

                Imgproc.rectangle(currentFrame,
                    new Point(rectX, rectY),
                    new Point(rectX + rectWidth, rectY + rectHeight),
                    new Scalar(0, 255, 0), 2);
            }
        }
        // If no face detected, keep the last known position

        grayFrame.release();
        smallFrame.release();
    }

    // Get absolute X position in game world (6 to 26)
    public float getAbsoluteX() {
        if (!isInitialized) {
            return (GAME_MIN_X + GAME_MAX_X) / 2f; // Return center if not initialized
        }

        // Map head X position (0 to cameraWidth) to game X (GAME_MIN_X to GAME_MAX_X)
        double normalizedX = 1 - (faceCenter.x / cameraWidth); //added an inversion to make it mirrored
        float gameX = GAME_MIN_X + (float)(normalizedX * (GAME_MAX_X - GAME_MIN_X));

        // Clamp to game boundaries
        return Math.max(GAME_MIN_X, Math.min(GAME_MAX_X, gameX));
    }

    // Get absolute Y position in game world (6 to 26)
    public float getAbsoluteY() {
        if (!isInitialized) {
            return (GAME_MIN_Y + GAME_MAX_Y) / 2f; // Return center if not initialized
        }

        // Map head Y position (0 to cameraHeight) to game Y (GAME_MIN_Y to GAME_MAX_Y)
        // Note: Camera Y is top-down, game Y is bottom-up, so we invert
        double invertedY = 1.0 - (faceCenter.y / cameraHeight);
        float gameY = GAME_MIN_Y + (float)(invertedY * (GAME_MAX_Y - GAME_MIN_Y));

        // Clamp to game boundaries
        return Math.max(GAME_MIN_Y, Math.min(GAME_MAX_Y, gameY));
    }

    private void updateCameraTexture() {
        if (rgbaFrame.empty()) return;

        // Create a mirrored version for display only
        Mat mirroredFrame = new Mat();
        Core.flip(rgbaFrame, mirroredFrame, 1);  // 1 = horizontal flip

        BufferedImage bufferedImage = matToBufferedImage(mirroredFrame);
        mirroredFrame.release();  // Clean up

        if (cameraTexture != null) {
            cameraTexture.dispose();
        }
        cameraTexture = bufferedImageToTexture(bufferedImage);
    }

    private BufferedImage matToBufferedImage(Mat mat) {
        int type = BufferedImage.TYPE_3BYTE_BGR;
        int bufferSize = mat.channels() * mat.cols() * mat.rows();
        byte[] buffer = new byte[bufferSize];
        mat.get(0, 0, buffer);

        BufferedImage image = new BufferedImage(mat.cols(), mat.rows(), type);
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(buffer, 0, targetPixels, 0, buffer.length);

        return image;
    }

    private Texture bufferedImageToTexture(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGB888);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                pixmap.setColor(r / 255f, g / 255f, b / 255f, 1f);
                pixmap.drawPixel(x,y);
            }
        }

        return new Texture(pixmap);
    }

    public Texture getCameraTexture() {
        return cameraTexture;
    }

    public void toggleCameraFeed() {
        showCameraFeed = !showCameraFeed;
        System.out.println("Camera feed: " + (showCameraFeed ? "ON" : "OFF"));
    }

    public boolean isCameraFeedEnabled() {
        return showCameraFeed;
    }

    public boolean isHeadTrackingEnabled() {
        return isInitialized;
    }

    public void dispose() {
        if (camera != null) {
            camera.release();
        }
        if (cameraTexture != null) {
            cameraTexture.dispose();
        }
        if (currentFrame != null) {
            currentFrame.release();
        }
        if (rgbaFrame != null) {
            rgbaFrame.release();
        }
    }
}
