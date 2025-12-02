package com.tutorial.game.gameComponenets.controllers;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class HandSignController implements Runnable {

    // Networking
    private DatagramSocket socket;
    private Thread listenerThread;
    private boolean isRunning;

    // Ports must match Python script
    private final int LISTEN_PORT = 5005;
    private final int SEND_PORT = 5006;
    private final String PYTHON_IP = "127.0.0.1";

    // Game Logic
    private String detectedSign;
    private String lastReceivedDirection;
    private float timeRemaining;
    private boolean isActive;

    private long lastReceiveTime;
    private final long TIMEOUT_MS = 500;

    public HandSignController() {
        this.isActive = false;
        this.detectedSign = "Waiting...";

        try {
            socket = new DatagramSocket(LISTEN_PORT);
            socket.setSoTimeout(100);

            isRunning = true;
            listenerThread = new Thread(this);
            listenerThread.start();
            System.out.println("✅ Java Listener started on Port " + LISTEN_PORT);

        } catch (SocketException e) {
            System.err.println("❌ Could not bind port " + LISTEN_PORT);
        }
    }

    // --- SEND COMMANDS TO PYTHON ---
    private void sendCommandToPython(String command) {
        try {
            byte[] data = command.getBytes();
            InetAddress address = InetAddress.getByName(PYTHON_IP);
            DatagramPacket packet = new DatagramPacket(data, data.length, address, SEND_PORT);
            socket.send(packet);
            System.out.println("📤 Sent to Python: " + command);
        } catch (Exception e) {
            System.err.println("Failed to send command to Python: " + e.getMessage());
        }
    }

    // --- LISTENER THREAD ---
    @Override
    public void run() {
        byte[] buffer = new byte[1024];

        while (isRunning && socket != null) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String message = new String(packet.getData(), 0, packet.getLength()).trim();
                lastReceivedDirection = message;
                lastReceiveTime = System.currentTimeMillis();

            } catch (Exception e) {
                // Timeout expected
            }
        }
    }

    public void startSpellCasting(String requiredDirection) {
        isActive = true;
        timeRemaining = 15f;
        detectedSign = "Show Hand!";

        // WAKE UP PYTHON
        sendCommandToPython("START");
    }

    public void stopSpellCasting() {
        if (!isActive) return;

        isActive = false;

        // PUT PYTHON TO SLEEP
        sendCommandToPython("STOP");
    }

    public void update() {
        if (!isActive) return;

        timeRemaining -= Gdx.graphics.getDeltaTime();
        if (timeRemaining <= 0) {
            stopSpellCasting();
            return;
        }

        if (System.currentTimeMillis() - lastReceiveTime < TIMEOUT_MS) {
            detectedSign = lastReceivedDirection;
        } else {
            detectedSign = null;
        }
    }

    public void draw(SpriteBatch batch, float x, float y, float width, float height) {
        // Camera feed is in Python window now, so we don't draw here.
    }

    public String getDetectedDirection() {
        if (detectedSign == null) return null;
        return detectedSign;
    }

    public String getDetectedSign() {
        return detectedSign != null ? detectedSign.toUpperCase() : "NONE";
    }

    public boolean hasDetectedDirection() {
        return detectedSign != null;
    }

    public float getTimeRemaining() {
        return timeRemaining;
    }

    public boolean isActive() {
        return isActive;
    }

    public void dispose() {
        isRunning = false;
        // Make sure Python stops if we close the game
        sendCommandToPython("STOP");

        if (socket != null) {
            socket.close();
        }
    }
}
