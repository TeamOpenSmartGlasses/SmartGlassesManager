import sys
import pygame, sys
from pygame.locals import *
from time import sleep

data = list()
with open(sys.argv[1], "r") as f:
    for i, line in enumerate(f):
        line = line.split("\t")[-1].strip().replace(" ", "").split("(")[-1].split(")")[0]
        xyz = line.split(",")
        x = float(xyz[0])
        y = float(xyz[1])
        data.append([x,y,i])

# set up pygame
pygame.init()
for i in [0]: #print every nth, so easier to see
    # set up the window
    width = 1920 
    height = 1080
#    eyebox_x = width * 0.14
#    eyebox_y = height * 0.35
#    eyebox_width = 390
#    eyebox_height = 120
#    width_scaler = width / eyebox_width
#    real_width = int(eyebox_width * width_scaler)
#    real_height = int(eyebox_height * width_scaler)
#    windowSurface = pygame.display.set_mode((real_width, real_height), 0, 32)
    windowSurface = pygame.display.set_mode((width, height), 0, 32)
    pygame.display.set_caption('Hello world!')
    # set up the colors
    BLACK = (0, 0, 0)
    WHITE = (255, 255, 255)
    RED = (255, 0, 0)
    GREEN = (0, 255, 0)
    BLUE = (0, 0, 255)
    windowSurface.fill(WHITE)

    # set up fonts
    basicFont = pygame.font.SysFont(None, 14)
    for datum in data:
        #if datum[2] % 3 == i:
        #run through coordinates
        # set up the text
        text = basicFont.render(str(datum[2]), True, RED, None)
        textRect = text.get_rect()
#        textRect.centerx = (real_width * datum[0]) #windowSurface.get_rect().centerx
#        textRect.centery = real_height * datum[1] #windowSurface.get_rect().centery

        textRect.centerx = width * datum[1] #windowSurface.get_rect().centerx
        textRect.centery = height * datum[0] #windowSurface.get_rect().centery
        # draw the white background onto the surface
        # draw the text onto the surface
        windowSurface.blit(text, textRect)
        # draw the window onto the screen
        pygame.display.update()
    #input()

# run the game loop
done = False
while not done:
    for event in pygame.event.get():
        if event.type == pygame.KEYDOWN:
            if event.key == pygame.K_ESCAPE:
                done = True
                break # break out of the for loop
        if event.type == QUIT:
                done = True
                break # break out of the for loop
    if done:
        pygame.quit()
