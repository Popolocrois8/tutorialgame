import cv2
import mediapipe as mp
import numpy as np
import math
import time
import socket
import threading
import json

class HandGestureServer:
    def __init__(self, host='localhost', port=12345):
        self.host = host
        self.port = port
        self.running = True

        # Initialize Mediapipe hand detection
        self.mp_hands = mp.solutions.hands
        self.hands = self.mp_hands.Hands(
            static_image_mode=False,
            max_num_hands=1,
            min_detection_confidence=0.7,
            min_tracking_confidence=0.7
        )
        self.mp_drawing = mp.solutions.drawing_utils

        # Open webcam
        self.cap = cv2.VideoCapture(0)
        self.cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
        self.cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)

        # Gesture recognition state
        self.last_gesture_time = 0
        self.gesture_cooldown = 1.0  # seconds between gesture detections

        # Socket setup
        self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.server_socket.bind((self.host, self.port))
        self.server_socket.listen(1)

        self.client_socket = None
        self.client_connected = False

        print(f"Hand gesture server started on {self.host}:{self.port}")

    def recognize_gesture(self, landmarks):
        """Recognize hand gestures based on finger positions"""
        # Get key landmarks
        thumb_tip = landmarks[4]
        index_tip = landmarks[8]
        middle_tip = landmarks[12]
        ring_tip = landmarks[16]
        pinky_tip = landmarks[20]
        wrist = landmarks[0]

        # Calculate finger distances from wrist
        thumb_dist = self.calculate_distance(thumb_tip.x, thumb_tip.y, wrist.x, wrist.y)
        index_dist = self.calculate_distance(index_tip.x, index_tip.y, wrist.x, wrist.y)
        middle_dist = self.calculate_distance(middle_tip.x, middle_tip.y, wrist.x, wrist.y)
        ring_dist = self.calculate_distance(ring_tip.x, ring_tip.y, wrist.x, wrist.y)
        pinky_dist = self.calculate_distance(pinky_tip.x, pinky_tip.y, wrist.x, wrist.y)

        # Check for open fingers (tip is higher than middle joint)
        thumb_open = thumb_tip.y < landmarks[3].y
        index_open = index_tip.y < landmarks[6].y
        middle_open = middle_tip.y < landmarks[10].y
        ring_open = ring_tip.y < landmarks[14].y
        pinky_open = pinky_tip.y < landmarks[18].y

        # Gesture recognition
        if index_open and middle_open and not ring_open and not pinky_open and not thumb_open:
            return "peace"  # ✌️
        elif index_open and not middle_open and not ring_open and not pinky_open and thumb_open:
            return "point"  # 👆
        elif all([thumb_open, index_open, middle_open, ring_open, pinky_open]):
            return "open_hand"  # 🖐️
        elif not any([thumb_open, index_open, middle_open, ring_open, pinky_open]):
            return "fist"  # ✊
        elif index_open and thumb_open and not middle_open and not ring_open and not pinky_open:
            return "pinch"  # 🤏
        elif index_open and middle_open and ring_open and pinky_open and not thumb_open:
            return "four_fingers"  # 🖖
        else:
            return "unknown"

    def calculate_distance(self, x1, y1, x2, y2):
        return math.sqrt((x2 - x1) ** 2 + (y2 - y1) ** 2)

    def send_data(self, hand_data):
        if self.client_connected and self.client_socket:
            try:
                data_str = json.dumps(hand_data)
                self.client_socket.send((data_str + '\n').encode())
            except:
                self.client_connected = False

    def handle_client(self, client_socket):
        self.client_socket = client_socket
        self.client_connected = True
        print("Java client connected!")

        try:
            while self.running and self.client_connected:
                ret, frame = self.cap.read()
                if not ret:
                    print("Failed to capture frame")
                    time.sleep(0.033)
                    continue

                # Flip frame horizontally for mirror-like interaction
                frame = cv2.flip(frame, 1)
                rgb_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)

                # Process the frame to detect hand landmarks
                result = self.hands.process(rgb_frame)

                # Prepare hand data for Java client
                hand_data = {
                    "landmarks": [],
                    "gesture": "none",
                    "index_tip": {"x": 0, "y": 0},
                    "pinch": False,
                    "spell": "none"  # New field for spell casting
                }

                # Process hand landmarks if detected
                if result.multi_hand_landmarks:
                    for hand_landmarks in result.multi_hand_landmarks:
                        # Convert landmarks to pixel coordinates
                        h, w, _ = frame.shape
                        landmarks_px = []
                        for landmark in hand_landmarks.landmark:
                            landmarks_px.append({
                                "x": landmark.x * w,
                                "y": landmark.y * h
                            })

                        hand_data["landmarks"] = landmarks_px

                        # Get index finger tip
                        index_tip = hand_landmarks.landmark[8]
                        hand_data["index_tip"] = {
                            "x": index_tip.x * w,
                            "y": index_tip.y * h
                        }

                        # Recognize gesture
                        current_gesture = self.recognize_gesture(hand_landmarks.landmark)
                        hand_data["gesture"] = current_gesture

                        # Map gestures to spells (with cooldown)
                        current_time = time.time()
                        if current_time - self.last_gesture_time > self.gesture_cooldown:
                            if current_gesture == "peace":
                                hand_data["spell"] = "fire"
                                print("🔥 FIRE SPELL CAST! (Peace gesture)")
                                self.last_gesture_time = current_time
                            elif current_gesture == "point":
                                hand_data["spell"] = "lightning"
                                print("⚡ LIGHTNING SPELL CAST! (Point gesture)")
                                self.last_gesture_time = current_time
                            elif current_gesture == "fist":
                                hand_data["spell"] = "earth"
                                print("🌍 EARTH SPELL CAST! (Fist gesture)")
                                self.last_gesture_time = current_time
                            elif current_gesture == "open_hand":
                                hand_data["spell"] = "water"
                                print("💧 WATER SPELL CAST! (Open hand gesture)")
                                self.last_gesture_time = current_time
                            elif current_gesture == "four_fingers":
                                hand_data["spell"] = "wind"
                                print("🌪️ WIND SPELL CAST! (Four fingers gesture)")
                                self.last_gesture_time = current_time

                        # Check for pinch (for compatibility)
                        thumb_tip = hand_landmarks.landmark[4]
                        distance = self.calculate_distance(
                            index_tip.x, index_tip.y,
                            thumb_tip.x, thumb_tip.y
                        )
                        hand_data["pinch"] = distance < 0.05

                        # Draw hand landmarks
                        self.mp_drawing.draw_landmarks(frame, hand_landmarks, self.mp_hands.HAND_CONNECTIONS)

                # Display gesture info on frame
                cv2.putText(frame, f"Gesture: {hand_data['gesture']}",
                            (20, 30), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 0), 2)
                cv2.putText(frame, f"Spell: {hand_data['spell']}",
                            (20, 60), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 255, 0), 2)

                # Send data to Java client
                self.send_data(hand_data)

                # Display the frame
                cv2.imshow("Hand Gesture Server", frame)

                # Exit the loop when 'q' is pressed
                if cv2.waitKey(1) & 0xFF == ord('q'):
                    self.running = False
                    break

        except Exception as e:
            print(f"Error in client handler: {e}")
        finally:
            self.client_connected = False
            if self.client_socket:
                self.client_socket.close()

    def start(self):
        print("Waiting for Java client to connect...")
        client_socket, addr = self.server_socket.accept()
        print(f"Java client connected from {addr}")

        client_thread = threading.Thread(target=self.handle_client, args=(client_socket,))
        client_thread.start()
        client_thread.join()

    def stop(self):
        self.running = False
        if self.cap:
            self.cap.release()
        cv2.destroyAllWindows()
        if self.server_socket:
            self.server_socket.close()
        if self.hands:
            self.hands.close()

if __name__ == "__main__":
    server = HandGestureServer()
    try:
        server.start()
    except KeyboardInterrupt:
        server.stop()
    except Exception as e:
        print(f"Server error: {e}")
        server.stop()
