package com.google.mediapipe.apps.wearableai.affectivecomputing;

//translate number to name for the stack of landmarks from pose, left hand, right hand
class PoseAndHandsTranslator {
    //pose
    public static int NOSE = 0;
    public static int LEFT_EYE_INNER = 1;
    public static int LEFT_EYE = 2;
    public static int LEFT_EYE_OUTER = 3;
    public static int RIGHT_EYE_INNER = 4;
    public static int RIGHT_EYE = 5;
    public static int RIGHT_EYE_OUTER = 6;
    public static int LEFT_EAR = 7;
    public static int RIGHT_EAR = 8;
    public static int MOUTH_LEFT = 9;
    public static int MOUTH_RIGHT = 10;
    public static int LEFT_SHOULDER = 11;
    public static int RIGHT_SHOULDER = 12;
    public static int LEFT_ELBOW = 13;
    public static int RIGHT_ELBOW = 14;
    public static int LEFT_WRIST = 15;
    public static int RIGHT_WRIST = 16;
    public static int LEFT_PINKY = 17;
    public static int RIGHT_PINKY = 18;
    public static int LEFT_INDEX = 19;
    public static int RIGHT_INDEX = 20;
    public static int LEFT_THUMB = 21;
    public static int RIGHT_THUMB = 22;
    public static int LEFT_HIP = 23;
    public static int RIGHT_HIP = 24;
    public static int LEFT_KNEE = 25;
    public static int RIGHT_KNEE = 26;
    public static int LEFT_ANKLE = 27;
    public static int RIGHT_ANKLE = 28;
    public static int LEFT_HEEL = 29;
    public static int RIGHT_HEEL = 30;
    public static int LEFT_FOOT_INDEX = 31;
    public static int RIGHT_FOOT_INDEX = 32;

    //left hand
    public static int LEFT_HAND_WRIST = 33;
    public static int LEFT_HAND_THUMB_CMC = 34;
    public static int LEFT_HAND_THUMB_MCP = 35;
    public static int LEFT_HAND_THUMB_IP = 36;
    public static int LEFT_HAND_THUMB_TIP = 37;
    public static int LEFT_HAND_INDEX_FINGER_MCP = 38;
    public static int LEFT_HAND_INDEX_FINGER_PIP = 39;
    public static int LEFT_HAND_INDEX_FINGER_DIP = 40;
    public static int LEFT_HAND_INDEX_FINGER_TIP = 41;
    public static int LEFT_HAND_MIDDLE_FINGER_MCP = 42;
    public static int LEFT_HAND_MIDDLE_FINGER_PIP = 43;
    public static int LEFT_HAND_MIDDLE_FINGER_DIP = 44;
    public static int LEFT_HAND_MIDDLE_FINGER_TIP = 45;
    public static int LEFT_HAND_RING_FINGER_MCP = 46;
    public static int LEFT_HAND_RING_FINGER_PIP = 47;
    public static int LEFT_HAND_RING_FINGER_DIP = 48;
    public static int LEFT_HAND_RING_FINGER_TIP = 49;
    public static int LEFT_HAND_PINKY_MCP = 50;
    public static int LEFT_HAND_PINKY_PIP = 51;
    public static int LEFT_HAND_PINKY_DIP = 52;
    public static int LEFT_HAND_PINKY_TIP = 53;

    //right hand
    public static int RIGHT_HAND_WRIST = 54;
    public static int RIGHT_HAND_THUMB_CMC = 55;
    public static int RIGHT_HAND_THUMB_MCP = 56;
    public static int RIGHT_HAND_THUMB_IP = 57;
    public static int RIGHT_HAND_THUMB_TIP = 58;
    public static int RIGHT_HAND_INDEX_FINGER_MCP = 59;
    public static int RIGHT_HAND_INDEX_FINGER_PIP = 60;
    public static int RIGHT_HAND_INDEX_FINGER_DIP = 61;
    public static int RIGHT_HAND_INDEX_FINGER_TIP = 62;
    public static int RIGHT_HAND_MIDDLE_FINGER_MCP = 63;
    public static int RIGHT_HAND_MIDDLE_FINGER_PIP = 64;
    public static int RIGHT_HAND_MIDDLE_FINGER_DIP = 65;
    public static int RIGHT_HAND_MIDDLE_FINGER_TIP = 66;
    public static int RIGHT_HAND_RING_FINGER_MCP = 67;
    public static int RIGHT_HAND_RING_FINGER_PIP = 68;
    public static int RIGHT_HAND_RING_FINGER_DIP = 69;
    public static int RIGHT_HAND_RING_FINGER_TIP = 70;
    public static int RIGHT_HAND_PINKY_MCP = 71;
    public static int RIGHT_HAND_PINKY_PIP = 72;
    public static int RIGHT_HAND_PINKY_DIP = 73;
    public static int RIGHT_HAND_PINKY_TIP = 74;
}
