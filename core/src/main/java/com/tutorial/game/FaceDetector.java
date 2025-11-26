package com.tutorial.game;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import org.opencv.highgui.HighGui;

public class FaceDetector {
    private CascadeClassifier faceDetector;
    private VideoCapture camera;
    private boolean cameraActive;

    // Tracking state
    private Point targetPosition;
    private boolean targetLocked;
    private Rect currentFace;
    private double detectionConfidence;

    // Projectile system
    private List<Projectile> projectiles = new CopyOnWriteArrayList<>();
    private Random random = new Random();
    private long lastProjectileTime = 0;
    private int projectilesDodged = 0;
    private int projectilesHit = 0;
    private boolean wasHitThisFrame = false;

    public FaceDetector() {
        nu.pattern.OpenCV.loadLocally();
        initializeFaceDetection(cascadePath);
        initializeCamera();
    }

    private void initializeFaceDetection(String cascadePath) {
        try {
            faceDetector = new CascadeClassifier(cascadePath); //changed to accept
        } catch (Exception e) {
            System.err.println("Failed to initialize face detection: " + e.getMessage());
        }
    }

    private void initializeCamera() {
        camera = new VideoCapture(0);
        cameraActive = camera.isOpened();
        targetPosition = new Point(0, 0);
        targetLocked = false;
    }

    public Mat processFrame() {
        if (!cameraActive) return null;

        Mat frame = new Mat();
        camera.read(frame);

        if (frame.empty()) {
            frame.release();
            return null;
        }

        // Mirror the camera
        Core.flip(frame, frame, 1);

        detectFaces(frame);
        updateProjectiles(frame.size());
        checkCollisions();

        Mat displayFrame = drawGameOverlay(frame);
        frame.release();

        spawnProjectiles();

        return displayFrame;
    }

    private void detectFaces(Mat frame) {
        Mat grayFrame = new Mat();
        List<Rect> faces = new ArrayList<>();

        try {
            Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
            Imgproc.equalizeHist(grayFrame, grayFrame);

            MatOfRect faceDetections = new MatOfRect();
            faceDetector.detectMultiScale(
                grayFrame, faceDetections, 1.1, 3, 0,
                new Size(100, 100), new Size(500, 500)
            );

            faces = faceDetections.toList();
            faceDetections.release();

        } catch (Exception e) {
            System.err.println("Error during face detection: " + e.getMessage());
        } finally {
            grayFrame.release();
        }

        updateTrackingState(faces, frame.size());
    }

    private void updateTrackingState(List<Rect> faces, Size frameSize) {
        if (faces.isEmpty()) {
            targetLocked = false;
            detectionConfidence = 0.0;
            return;
        }

        Rect largestFace = faces.get(0);
        double maxArea = largestFace.area();

        for (Rect face : faces) {
            if (face.area() > maxArea) {
                maxArea = face.area();
                largestFace = face;
            }
        }

        currentFace = largestFace;
        targetPosition.x = currentFace.x + currentFace.width / 2.0;
        targetPosition.y = currentFace.y + currentFace.height / 2.0;
        targetLocked = true;

        double frameArea = frameSize.width * frameSize.height;
        detectionConfidence = Math.min(1.0, maxArea / (frameArea * 0.1));
    }

    private void spawnProjectiles() {
        long currentTime = System.currentTimeMillis();

        // Spawn new projectile every 0.5-1.5 seconds
        if (currentTime - lastProjectileTime > 500 + random.nextInt(1000)) {
            Projectile projectile = createProjectile();
            projectiles.add(projectile);
            lastProjectileTime = currentTime;
        }
    }

    private Projectile createProjectile() {
        Size frameSize = new Size(640, 480); // Adjust based on your camera resolution
        int side = random.nextInt(4); // 0: top, 1: right, 2: bottom, 3: left

        double x = 0, y = 0;
        double vx = 0, vy = 0;

        switch (side) {
            case 0: // Top
                x = random.nextDouble() * frameSize.width;
                y = -20;
                vx = (random.nextDouble() - 0.5) * 2;
                vy = random.nextDouble() * 2 + 1;
                break;
            case 1: // Right
                x = frameSize.width + 20;
                y = random.nextDouble() * frameSize.height;
                vx = -random.nextDouble() * 2 - 1;
                vy = (random.nextDouble() - 0.5) * 2;
                break;
            case 2: // Bottom
                x = random.nextDouble() * frameSize.width;
                y = frameSize.height + 20;
                vx = (random.nextDouble() - 0.5) * 2;
                vy = -random.nextDouble() * 2 - 1;
                break;
            case 3: // Left
                x = -20;
                y = random.nextDouble() * frameSize.height;
                vx = random.nextDouble() * 2 + 1;
                vy = (random.nextDouble() - 0.5) * 2;
                break;
        }

        return new Projectile(x, y, vx, vy);
    }

    private void updateProjectiles(Size frameSize) {
        for (Projectile projectile : projectiles) {
            projectile.update();

            // Remove projectiles that are off-screen
            if (projectile.x < -50 || projectile.x > frameSize.width + 50 ||
                projectile.y < -50 || projectile.y > frameSize.height + 50) {
                projectiles.remove(projectile);
                projectilesDodged++;
            }
        }
    }

    private void checkCollisions() {
        wasHitThisFrame = false;

        if (!targetLocked) return;

        for (Projectile projectile : projectiles) {
            double distance = Math.sqrt(
                Math.pow(projectile.x - targetPosition.x, 2) +
                    Math.pow(projectile.y - targetPosition.y, 2)
            );

            if (distance < 30) { // Collision detection radius
                projectiles.remove(projectile);
                projectilesHit++;
                wasHitThisFrame = true;
                break; // Only count one hit per frame
            }
        }
    }

    private Mat drawGameOverlay(Mat frame) {
        Mat displayFrame = frame.clone();

        // Draw face tracking
        if (targetLocked) {
            // Draw face rectangle
            Imgproc.rectangle(
                displayFrame,
                new Point(currentFace.x, currentFace.y),
                new Point(currentFace.x + currentFace.width, currentFace.y + currentFace.height),
                new Scalar(0, 255, 0), 2
            );

            // Draw player hitbox
            Imgproc.circle(displayFrame, targetPosition, 30,
                wasHitThisFrame ? new Scalar(0, 0, 255) : new Scalar(0, 255, 255), 2);
        }

        // Draw projectiles
        for (Projectile projectile : projectiles) {
            projectile.draw(displayFrame);
        }

        // Draw HUD
        drawHUD(displayFrame);

        return displayFrame;
    }

    private void drawHUD(Mat frame) {
        Scalar textColor = new Scalar(255, 255, 255);
        Scalar hitColor = new Scalar(0, 0, 255);
        Scalar dodgeColor = new Scalar(0, 255, 0);

        // Stats
        String stats = String.format("Hits: %d | Dodged: %d", projectilesHit, projectilesDodged);
        Imgproc.putText(frame, stats,
            new Point(20, 30),
            Imgproc.FONT_HERSHEY_SIMPLEX, 0.6, textColor, 2);

        // Hit feedback
        if (wasHitThisFrame) {
            Imgproc.putText(frame, "HIT!",
                new Point(frame.cols() / 2 - 30, 60),
                Imgproc.FONT_HERSHEY_SIMPLEX, 1.0, hitColor, 3);
        }

        // Instructions
        Imgproc.putText(frame, "Dodge the red projectiles!",
            new Point(20, frame.rows() - 20),
            Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, textColor, 1);
    }

    // Projectile class
    private class Projectile {
        double x, y;
        double vx, vy;
        long spawnTime;

        Projectile(double x, double y, double vx, double vy) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.spawnTime = System.currentTimeMillis();
        }

        void update() {
            x += vx;
            y += vy;
        }

        void draw(Mat frame) {
            // Draw projectile as red circle
            Imgproc.circle(frame, new Point(x, y), 8, new Scalar(0, 0, 255), -1);
            // Draw trail
            Imgproc.circle(frame, new Point(x, y), 12, new Scalar(0, 100, 255), 2);
        }
    }

    // Public methods for game integration
    public Point getTargetPosition() { return targetPosition.clone(); }
    public boolean isTargetLocked() { return targetLocked; }
    public boolean wasHitThisFrame() { return wasHitThisFrame; }
    public int getProjectilesHit() { return projectilesHit; }
    public int getProjectilesDodged() { return projectilesDodged; }

    // Reset game state
    public void resetGame() {
        projectiles.clear();
        projectilesHit = 0;
        projectilesDodged = 0;
        wasHitThisFrame = false;
    }

    public void cleanup() {
        if (camera != null && camera.isOpened()) {
            camera.release();
        }
    }


    // Add this to the end of your FaceDetector.java file:
    public static void main(String[] args) {
        FaceDetector detector = new FaceDetector();

        // Wait for camera to initialize
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        while (true) {
            Mat frame = detector.processFrame();
            if (frame != null && !frame.empty()) {  // Add empty check
                HighGui.imshow("Face Tracking Game", frame);

                int key = HighGui.waitKey(1);
                if (key == 27) break; // ESC to exit

                frame.release();
            } else {
                System.out.println("No frame available, waiting...");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        detector.cleanup();
        HighGui.destroyAllWindows();
    }
}


