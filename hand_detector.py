# hand_detector.py
import cv2
import mediapipe as mp
import numpy as np
import json
import sys
import time

# Initialize Mediapipe
mp_hands = mp.solutions.hands
hands = mp_hands.Hands(
    static_image_mode=False,
    max_num_hands=1,
    min_detection_confidence=0.7,
    min_tracking_confidence=0.7
)

# Open webcam
cap = cv2.VideoCapture(0)
cap.set(cv2.CAP_PROP_FRAME_WIDTH, 320)
cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 240)

# Drawing variables
drawing_canvas = None
is_drawing = False
last_point = None
drawn_points = []

def process_frame():
    global drawing_canvas, is_drawing, last_point, drawn_points

    ret, frame = cap.read()
    if not ret:
        return None

    # Flip for mirror view
    frame = cv2.flip(frame, 1)

    if drawing_canvas is None:
        drawing_canvas = np.zeros_like(frame)

    # Convert to RGB for Mediapipe
    rgb_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
    result = hands.process(rgb_frame)

    hand_detected = False
    index_tip = None
    thumb_tip = None

    if result.multi_hand_landmarks:
        hand_detected = True
        for hand_landmarks in result.multi_hand_landmarks:
            # Get index finger tip (landmark 8) and thumb tip (landmark 4)
            h, w, _ = frame.shape
            index_tip = (
                int(hand_landmarks.landmark[8].x * w),
                int(hand_landmarks.landmark[8].y * h)
            )
            thumb_tip = (
                int(hand_landmarks.landmark[4].x * w),
                int(hand_landmarks.landmark[4].y * h)
            )

            # Calculate distance between index and thumb
            if index_tip and thumb_tip:
                distance = np.sqrt((index_tip[0] - thumb_tip[0])**2 +
                                   (index_tip[1] - thumb_tip[1])**2)

                # Check if fingers are touching (pinch gesture)
                if distance < 40:  # Threshold for pinch
                    if not is_drawing:
                        is_drawing = True
                        drawn_points = []
                        last_point = None
                else:
                    if is_drawing:
                        is_drawing = False
                        # Analyze the drawn shape
                        shape = analyze_shape(drawn_points)
                        print(json.dumps({
                            "event": "shape_detected",
                            "shape": shape,
                            "points": len(drawn_points)
                        }), flush=True)

            # Draw on canvas if drawing
            if is_drawing and index_tip:
                drawn_points.append(index_tip)
                if last_point:
                    cv2.line(drawing_canvas, last_point, index_tip, (0, 0, 255), 3)
                last_point = index_tip

                # Draw current point
                cv2.circle(drawing_canvas, index_tip, 5, (255, 0, 0), -1)

    # Combine frame and canvas
    display_frame = cv2.addWeighted(frame, 0.7, drawing_canvas, 0.3, 0)

    # Draw status
    status = "DRAWING" if is_drawing else "MOVE HAND"
    cv2.putText(display_frame, status, (10, 30),
                cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 255), 2)

    # Convert to bytes for Java
    _, buffer = cv2.imencode('.jpg', display_frame, [cv2.IMWRITE_JPEG_QUALITY, 70])
    frame_bytes = buffer.tobytes()

    return {
        "hand_detected": hand_detected,
        "frame": frame_bytes,
        "is_drawing": is_drawing,
        "drawn_points": len(drawn_points)
    }

def analyze_shape(points):
    if len(points) < 10:
        return "unknown"

    # Convert to numpy array
    points_array = np.array(points)

    # Get bounding box
    min_x, min_y = np.min(points_array, axis=0)
    max_x, max_y = np.max(points_array, axis=0)

    width = max_x - min_x
    height = max_y - min_y
    aspect_ratio = width / height if height > 0 else 0

    # Simple shape detection (same as before)
    if aspect_ratio > 1.5:
        return "right"  # Horizontal line
    elif aspect_ratio < 0.66:
        return "up"     # Vertical line
    elif width > 50 and height > 50:
        if height > width * 1.3:
            return "down"  # Tall shape
        else:
            return "left"  # Square-ish
    else:
        return "unknown"

def clear_canvas():
    global drawing_canvas, drawn_points, is_drawing, last_point
    if drawing_canvas is not None:
        drawing_canvas = np.zeros_like(drawing_canvas)
    drawn_points = []
    is_drawing = False
    last_point = None
    print(json.dumps({"event": "canvas_cleared"}), flush=True)

def main():
    print("Hand Detector started", flush=True)

    while True:
        try:
            # Read command from Java
            if sys.stdin.readable():
                command = sys.stdin.readline().strip()
                if command:
                    if command == "CLEAR":
                        clear_canvas()
                    elif command == "EXIT":
                        break

            # Process frame
            result = process_frame()
            if result:
                # Send result to Java
                print(json.dumps(result), flush=True)

            time.sleep(0.03)  # ~30 FPS

        except Exception as e:
            print(json.dumps({"error": str(e)}), flush=True)
            time.sleep(1)

if __name__ == "__main__":
    main()
    cap.release()
