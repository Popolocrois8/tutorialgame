package com.tutorial.game.gameComponenets.controllers;

import org.opencv.core.*;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;
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
    private Point neutralPosition;
    private boolean isInitialized;
    private final float movementSensitivity;
    private final int movementThreshold;

    // For camera feed texture
    private Texture cameraTexture;
    private boolean showCameraFeed = true;

    public HeadMovementController() {
        this.movementSensitivity = 0.1f;
        this.movementThreshold = 20;
        this.faceCenter = new Point(0, 0);
        this.neutralPosition = new Point(0, 0);
        this.isInitialized = false;
        initializeCamera();
    }

    private void initializeCamera() {
        try {
            System.out.println("=== Initializing Head Movement Controller ===");
            System.out.println("Current working directory: " + System.getProperty("user.dir"));

            // Get the project root path (go up one level from assets)
            String projectRoot = new File(System.getProperty("user.dir")).getParent();
            if (projectRoot == null) {
                projectRoot = System.getProperty("user.dir");
            }
            System.out.println("Project root: " + projectRoot);

            // Try loading from different possible locations with correct paths
            String[] possibleDllPaths = {
                projectRoot + "\\libs\\opencv_java451.dll",  // Project root/libs folder
                projectRoot + "\\opencv_java451.dll",        // Project root
                "C:\\Users\\Aiman\\Documents\\Project\\tutorialgame\\libs\\opencv_java451.dll",  // Your exact path
                System.getProperty("user.dir") + "\\..\\libs\\opencv_java451.dll",  // Go up from assets
                System.getProperty("user.dir") + "\\..\\opencv_java451.dll",        // Go up from assets
                "libs\\opencv_java451.dll",
                "opencv_java451.dll"
            };

            boolean dllLoaded = false;
            for (String dllPath : possibleDllPaths) {
                try {
                    System.out.println("Trying to load: " + dllPath);
                    File dllFile = new File(dllPath);
                    if (dllFile.exists()) {
                        System.out.println("✅ DLL exists at: " + dllPath);
                        System.load(dllPath);
                        System.out.println("✅ SUCCESS: Loaded OpenCV from: " + dllPath);
                        dllLoaded = true;
                        break;
                    } else {
                        System.out.println("❌ DLL not found at: " + dllPath);
                    }
                } catch (UnsatisfiedLinkError e) {
                    System.out.println("❌ Failed to load from: " + dllPath + " - " + e.getMessage());
                }
            }

            if (!dllLoaded) {
                System.err.println("❌ CRITICAL: Could not load OpenCV DLL from any location");

                // Show what's actually in the libs folder
                File libsFolder = new File(projectRoot + "\\libs");
                if (libsFolder.exists()) {
                    System.out.println("Contents of libs folder:");
                    String[] libFiles = libsFolder.list();
                    if (libFiles != null) {
                        for (String file : libFiles) {
                            System.out.println("  " + file);
                        }
                    }
                } else {
                    System.out.println("Libs folder does not exist: " + libsFolder.getAbsolutePath());
                }
                return;
            }

            // Rest of your initialization code...
            System.out.println("OpenCV version: " + Core.VERSION);

            camera = new VideoCapture(0);
            if (!camera.isOpened()) {
                System.err.println("❌ Camera not accessible");
                return;
            }
            System.out.println("✅ Camera opened successfully");

            // Initialize face detector with correct paths
            faceDetector = new CascadeClassifier();
            String[] cascadePaths = {
                projectRoot + "\\assets\\haarcascade_frontalface_alt.xml",
                projectRoot + "\\haarcascade_frontalface_alt.xml",
                "haarcascade_frontalface_alt.xml",
                "assets/haarcascade_frontalface_alt.xml"
            };

            boolean cascadeLoaded = false;
            for (String path : cascadePaths) {
                File cascadeFile = new File(path);
                if (cascadeFile.exists()) {
                    System.out.println("✅ Cascade file exists at: " + path);
                    cascadeLoaded = faceDetector.load(path);
                    if (cascadeLoaded) {
                        System.out.println("✅ Loaded face detector from: " + path);
                        break;
                    }
                } else {
                    System.out.println("❌ Cascade file not found at: " + path);
                }
            }

            if (!cascadeLoaded) {
                System.err.println("❌ Could not load face detector");
                return;
            }

            currentFrame = new Mat();
            rgbaFrame = new Mat();

            if (camera.read(currentFrame) && !currentFrame.empty()) {
                calibrateNeutralPosition();
                isInitialized = true;
                System.out.println("✅ Camera initialized: " + currentFrame.width() + "x" + currentFrame.height());
            }

        } catch (Exception e) {
            System.err.println("❌ Error initializing camera: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void calibrateNeutralPosition() {
        if (currentFrame != null && !currentFrame.empty()) {
            neutralPosition = new Point(currentFrame.width() / 2.0, currentFrame.height() / 2.0);
            faceCenter = new Point(neutralPosition.x, neutralPosition.y);
            System.out.println("Neutral position calibrated: " + neutralPosition);
        }
    }

    public void updateHeadPosition() {
        if (!isInitialized || camera == null) return;

        try {
            // Read frame from camera
            if (camera.read(currentFrame) && !currentFrame.empty()) {
                // Convert to RGB for processing
                Imgproc.cvtColor(currentFrame, rgbaFrame, Imgproc.COLOR_BGR2RGB);

                // Detect faces
                detectFace();

                // Update camera texture for rendering
                updateCameraTexture();
            }
        } catch (Exception e) {
            System.err.println("Error updating head position: " + e.getMessage());
        }
    }

    private void detectFace() {
        MatOfRect faceDetections = new MatOfRect();
        Mat grayFrame = new Mat();

        // Convert to grayscale for face detection
        Imgproc.cvtColor(currentFrame, grayFrame, Imgproc.COLOR_BGR2GRAY);
        Imgproc.equalizeHist(grayFrame, grayFrame);

        // Detect faces
        faceDetector.detectMultiScale(grayFrame, faceDetections);

        Rect[] facesArray = faceDetections.toArray();
        if (facesArray.length > 0) {
            // Use the largest face
            Rect face = facesArray[0];
            for (Rect f : facesArray) {
                if (f.width * f.height > face.width * face.height) {
                    face = f;
                }
            }

            // Calculate face center
            faceCenter = new Point(face.x + face.width / 2.0, face.y + face.height / 2.0);

            // Draw rectangle around face on the frame (for visualization)
            Imgproc.rectangle(rgbaFrame,
                new Point(face.x, face.y),
                new Point(face.x + face.width, face.y + face.height),
                new Scalar(0, 255, 0), 3);

            // Draw crosshair at face center
            Imgproc.line(rgbaFrame,
                new Point(faceCenter.x - 10, faceCenter.y),
                new Point(faceCenter.x + 10, faceCenter.y),
                new Scalar(255, 0, 0), 2);
            Imgproc.line(rgbaFrame,
                new Point(faceCenter.x, faceCenter.y - 10),
                new Point(faceCenter.x, faceCenter.y + 10),
                new Scalar(255, 0, 0), 2);

        } else {
            // No face detected
            faceCenter = new Point(neutralPosition.x, neutralPosition.y);
        }

        grayFrame.release();
    }

    private void updateCameraTexture() {
        if (rgbaFrame.empty()) return;

        // Convert OpenCV Mat to BufferedImage
        BufferedImage bufferedImage = matToBufferedImage(rgbaFrame);

        // Convert to LibGDX Texture
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
                pixmap.drawPixel(x, height - 1 - y); // Flip Y coordinate
            }
        }

        return new Texture(pixmap);
    }

    public float getHorizontalMovement() {
        if (!isInitialized || faceCenter == null || neutralPosition == null) {
            return 0;
        }

        double deltaX = faceCenter.x - neutralPosition.x;

        // Apply threshold to avoid jitter
        if (Math.abs(deltaX) < movementThreshold) {
            return 0;
        }

        // Normalize and apply sensitivity
        float movement = (float) (deltaX / neutralPosition.x * movementSensitivity);
        return Math.max(-1, Math.min(1, movement));
    }

    public float getVerticalMovement() {
        if (!isInitialized || faceCenter == null || neutralPosition == null) {
            return 0;
        }

        double deltaY = faceCenter.y - neutralPosition.y;

        // Apply threshold to avoid jitter
        if (Math.abs(deltaY) < movementThreshold) {
            return 0;
        }

        // Normalize and apply sensitivity (invert Y for intuitive movement)
        float movement = (float) (-deltaY / neutralPosition.y * movementSensitivity);
        return Math.max(-1, Math.min(1, movement));
    }

    public Texture getCameraTexture() {
        return cameraTexture;
    }

    public Point getFaceCenter() {
        return faceCenter;
    }

    public Point getNeutralPosition() {
        return neutralPosition;
    }

    public void toggleCameraFeed() {
        showCameraFeed = !showCameraFeed;
    }

    public boolean isCameraFeedEnabled() {
        return showCameraFeed;
    }

    public boolean isHeadTrackingEnabled() {
        return isInitialized;
    }

    public void recalibrate() {
        calibrateNeutralPosition();
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
