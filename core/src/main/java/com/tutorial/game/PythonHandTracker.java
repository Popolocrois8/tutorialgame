package com.tutorial.game;

import org.opencv.core.*;
import org.opencv.videoio.VideoCapture;
import org.opencv.imgproc.Imgproc;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class PythonHandTracker {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private Gson gson;
    private HandData currentHandData;
    private boolean connected = false;

    // Drawing state
    private List<Point> drawingPoints = new ArrayList<>();
    private boolean isDrawing = false;
    private long lastSpellTime = 0;

    public PythonHandTracker() {
        this.gson = new Gson();
        this.currentHandData = new HandData();
    }

    public boolean connect(String host, int port) {
        try {
            socket = new Socket(host, port);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
            connected = true;

            // Start data reading thread
            Thread dataThread = new Thread(this::readData);
            dataThread.setDaemon(true);
            dataThread.start();

            System.out.println("Connected to Python hand tracker server");
            return true;

        } catch (IOException e) {
            System.err.println("Failed to connect to Python server: " + e.getMessage());
            return false;
        }
    }

    private void readData() {
        try {
            String line;
            while (connected && (line = reader.readLine()) != null) {
                try {
                    JsonObject json = JsonParser.parseString(line).getAsJsonObject();
                    currentHandData = gson.fromJson(json, HandData.class);

                    // Update drawing state based on pinch gesture
                    if (currentHandData.pinch && !isDrawing) {
                        isDrawing = true;
                        drawingPoints.clear();
                        System.out.println("Drawing started (pinch detected)");
                    } else if (!currentHandData.pinch && isDrawing) {
                        isDrawing = false;
                        System.out.println("Drawing stopped");
                    }

                    // Add point to drawing if in drawing mode
                    if (isDrawing && currentHandData.indexTip != null) {
                        Point drawingPoint = new Point(
                            currentHandData.indexTip.x,
                            currentHandData.indexTip.y
                        );
                        drawingPoints.add(drawingPoint);

                        // Limit drawing points
                        if (drawingPoints.size() > 50) {
                            drawingPoints.remove(0);
                        }
                    }

                } catch (Exception e) {
                    System.err.println("Error parsing hand data: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Disconnected from Python server: " + e.getMessage());
            connected = false;
        }
    }

    public Mat processFrame(Mat frame) {
        if (!connected) {
            // Fallback: draw connection message
            Imgproc.putText(frame, "Not connected to Python server",
                new Point(20, 30), Imgproc.FONT_HERSHEY_SIMPLEX, 0.6,
                new Scalar(0, 0, 255), 2);
            return frame;
        }

        Mat displayFrame = frame.clone();

        // Draw hand landmarks if available
        if (currentHandData.landmarks != null && !currentHandData.landmarks.isEmpty()) {
            drawHandLandmarks(displayFrame, currentHandData.landmarks);
        }

        // Draw drawing trail
        for (int i = 1; i < drawingPoints.size(); i++) {
            Point prev = drawingPoints.get(i - 1);
            Point curr = drawingPoints.get(i);
            Imgproc.line(displayFrame, prev, curr, new Scalar(0, 0, 255), 4);
        }

        // Draw current gesture info
        drawGestureInfo(displayFrame);

        return displayFrame;
    }

    private void drawHandLandmarks(Mat frame, List<Landmark> landmarks) {
        // Draw landmarks
        for (Landmark landmark : landmarks) {
            Point point = new Point(landmark.x, landmark.y);
            Imgproc.circle(frame, point, 3, new Scalar(0, 255, 0), -1);
        }

        // Draw connections (simplified)
        int[][] connections = {
            {0, 1}, {1, 2}, {2, 3}, {3, 4}, // thumb
            {0, 5}, {5, 6}, {6, 7}, {7, 8}, // index
            {0, 9}, {9, 10}, {10, 11}, {11, 12}, // middle
            {0, 13}, {13, 14}, {14, 15}, {15, 16}, // ring
            {0, 17}, {17, 18}, {18, 19}, {19, 20} // pinky
        };

        for (int[] connection : connections) {
            if (connection[0] < landmarks.size() && connection[1] < landmarks.size()) {
                Point p1 = new Point(landmarks.get(connection[0]).x, landmarks.get(connection[0]).y);
                Point p2 = new Point(landmarks.get(connection[1]).x, landmarks.get(connection[1]).y);
                Imgproc.line(frame, p1, p2, new Scalar(0, 255, 0), 2);
            }
        }
    }

    private void drawGestureInfo(Mat frame) {
        Scalar textColor = new Scalar(255, 255, 255);
        Scalar gestureColor = currentHandData.pinch ? new Scalar(0, 255, 255) : new Scalar(0, 255, 0);

        String gestureText = "Gesture: " + currentHandData.gesture;
        if (currentHandData.pinch) {
            gestureText += " (DRAWING)";
        }

        Imgproc.putText(frame, gestureText,
            new Point(20, 30), Imgproc.FONT_HERSHEY_SIMPLEX, 0.6, gestureColor, 2);

        Imgproc.putText(frame, "Pinch to draw spells in air",
            new Point(20, 60), Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, textColor, 1);
    }

    // Spell recognition (same as before)
    public int recognizeSpell() {
        if (drawingPoints.size() < 8) return -1;

        // Your existing spell recognition logic here
        // Check for square, circle, triangle, cross

        return -1; // No spell recognized
    }

    public void castSpell(int spellType) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSpellTime < 2000) return;

        lastSpellTime = currentTime;
        drawingPoints.clear();

        // Your existing spell casting logic here
        System.out.println("Spell cast: " + spellType);
    }

    public void disconnect() {
        connected = false;
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Error disconnecting: " + e.getMessage());
        }
    }

    // Data classes
    public static class HandData {
        public List<Landmark> landmarks = new ArrayList<>();
        public String gesture = "none";
        public Landmark indexTip;
        public boolean pinch = false;
    }

    public static class Landmark {
        public double x;
        public double y;
    }
}
