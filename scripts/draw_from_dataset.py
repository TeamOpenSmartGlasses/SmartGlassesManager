import sys
import pygame, sys
from pygame.locals import *
from time import sleep
import pandas as pd
import numpy as np


#load the cvs
df = pd.read_csv(sys.argv[1], header=None)
names = df.iloc[:,0].to_numpy().tolist()

# set up pygame
pygame.init()
for i in [0]: #print every nth, so easier to see
    # set up the window
    width = 1820 
    height = 1000
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
    for j, name in enumerate(names):
        if "0003" not in name or "_0P" not in name:
            continue
        data = df.iloc[j, 1:].to_numpy()
        pygame.display.set_caption(name)
        for k, datum in enumerate(data[:478]):
            #if datum[2] % 3 == i:
            #run through coordinates
            # set up the text
            text = basicFont.render(str(k*2), True, RED, None)
            textRect = text.get_rect()
    #        textRect.centerx = (real_width * datum[0]) #windowSurface.get_rect().centerx
    #        textRect.centery = real_height * datum[1] #windowSurface.get_rect().centery

            textRect.centerx = width * data[k*2] #windowSurface.get_rect().centerx
            textRect.centery = height * data[(k*2)+1] #windowSurface.get_rect().centery
            # draw the white background onto the surface
            # draw the text onto the surface
            windowSurface.blit(text, textRect)
            # draw the window onto the screen
        pygame.display.update()
        windowSurface.fill(WHITE)
        input()

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
