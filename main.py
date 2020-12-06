import sys
import cv2

from face_analysis.face_analysis import FaceAnalyze

"""
NOTE: This main script is all working in BGR (that's what detectron2 is using). If we use another network in RGB or something else, then we will convert it in the pass_frame function of the wrapping class for the model's inference.
"""

video_file = sys.argv[1]
#load sample image

#start models
face_analyze = FaceAnalyze()

#open video file/stream
vid = cv2.VideoCapture(video_file)
fps = vid.get(cv2.CAP_PROP_FPS)
period = 1 / fps
video_time = 0

def get_vec(img):
    """
    img : cv2 image, ussually a frame from video

    Returns:
    output of a number of computer vision / hearing algorithms
    """
    #pass into facial analysis class
    face_analyze.pass_frame(img)

    #get facial attributes
    face_atts = face_analyze.get_attributes()

    return face_atts

#loop through frames of video
while True:
    ret, frame = vid.read()
    if not ret:
        break
    video_time = video_time + period
    #pass through pipeline
    hl_vec = get_vec(frame)
    print(hl_vec)
    print("At video time: {}".format(video_time))
