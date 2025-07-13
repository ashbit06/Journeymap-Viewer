import os, json
from json import JSONDecodeError

import pygame as pg
import pygame_gui as gui

from journeymap import *


DEFAULT_WINDOW_WIDTH = 800
DEFAULT_WINDOW_HEIGHT = 500
TOOLBAR_HEIGHT = 30
REGION_SIZE = 512

BACKGROUND_COLOR = (255, 255, 255)
MENU_BAR_COLOR = (0, 0, 0)


with open("data.json") as file:
    try: DATA: dict = json.loads("".join(file.readlines()))
    except JSONDecodeError: DATA = dict()

if not DATA:
    DATA["window"] = dict()
    DATA["window"]["position"] = [0, 0]
    DATA["window"]["size"] = (DEFAULT_WINDOW_WIDTH, DEFAULT_WINDOW_HEIGHT)
    DATA["journeymap"] = dict()
    DATA["journeymap"]["root"] = ""
    DATA["journeymap"]["world"] = ""


# initialize window
pg.init()
screen = pg.display.set_mode((DEFAULT_WINDOW_WIDTH, DEFAULT_WINDOW_HEIGHT) if not DATA
                             else DATA["window"]["size"])
if DATA: pg.display.set_window_position(DATA["window"]["position"])
pg.display.set_caption("Journeymap Viewer")
icon = pg.image.load("./assets/icon.png")
pg.display.set_icon(icon)
ui_manager = gui.UIManager((DEFAULT_WINDOW_WIDTH, DEFAULT_WINDOW_HEIGHT))

running = True
clock = pg.time.Clock()
time_delta = 0
keys_pressed = set()

journeymap: Journeymap|None = Journeymap("/Users/ashton/Library/Application Support/ModrinthApp/profiles/1.21.4 fabric/journeymap",
                                         "mp/wild~survival")
show_map = False
current_world: str|None = None
current_dimension = Dimension.OVERWORLD
current_maptype = MapType.DAY
cached_regions: set[tuple[int, int, pg.Surface]] = set()

pan_x: float = 0    # DATA["window"]["size"][0] / 2
pan_y: float = 0    # DATA["window"]["size"][1] / 2
zoom: float = 0.5


def draw_regions():
    # (x1, y1, x2, y2)
    viewable_rectangle = (
        pan_x,
        pan_y + TOOLBAR_HEIGHT,
        pan_x + screen.get_width(),
        pan_y + screen.get_height()
    )

    # get which regions are viewable on the edges
    viewable_topLeft     = (int(viewable_rectangle[0] / (REGION_SIZE * zoom)),
                            int(viewable_rectangle[1] / (REGION_SIZE * zoom)))
    viewable_bottomRight = (int(viewable_rectangle[2] / (REGION_SIZE * zoom))+2,
                            int(viewable_rectangle[3] / (REGION_SIZE * zoom)))

    print(f"{pan_x=}, {pan_y=} \t{viewable_topLeft=}, {viewable_bottomRight=}")




    y = viewable_rectangle[1] % (REGION_SIZE * zoom) - (REGION_SIZE * zoom)
    for r in range(viewable_topLeft[0], viewable_bottomRight[0]):
        x = viewable_rectangle[0] % (REGION_SIZE * zoom) - (REGION_SIZE * zoom)
        for c in range(viewable_topLeft[0], viewable_bottomRight[0]):
            try:
                region = journeymap.get_region(current_dimension    , current_maptype, (c, r))
                scaled = pg.transform.scale(region, (REGION_SIZE*zoom,REGION_SIZE*zoom))
                screen.blit(scaled, (x,y))
            except FileNotFoundError:
                print(f"Region {c},{r} hasn't been mapped yet")
            x += REGION_SIZE * zoom
        y += REGION_SIZE * zoom



while running:
    for event in pg.event.get():
        if event.type == pg.QUIT:
            running = False
        elif event.type == pg.KEYDOWN:
            keys_pressed.add(event.key)
        elif event.type == pg.KEYUP:
            keys_pressed.remove(event.key)

        ui_manager.process_events(event)

    mouse_x, mouse_y = pg.mouse.get_pos()
    wheel_delta = pg.mouse
    print(wheel_delta)

    if pg.K_UP in keys_pressed:
        pan_y += 16.0
    if pg.K_DOWN in keys_pressed:
        pan_y -= 16.0
    if pg.K_LEFT in keys_pressed:
        pan_x += 16.0
    if pg.K_RIGHT in keys_pressed:
        pan_x -= 16.0
    if pg.K_EQUALS in keys_pressed:
        zoom += 0.1
    if pg.K_MINUS in keys_pressed:
        zoom -= 0.1

    if zoom < 0.1: zoom = 0.1
    if zoom > 4: zoom = 4.0

    screen.fill(BACKGROUND_COLOR)

    draw_regions()
    # screen.blit(journeymap.get_region(Dimension.OVERWORLD, MapType.DAY, (0,0)), (pan_x, pan_y))


    ui_manager.draw_ui(screen)
    ui_manager.update(time_delta)
    pg.display.flip()
    time_delta = clock.tick(30) / 1000


with open("data.json", "w") as file:
    DATA["window"]["position"] = pg.display.get_window_position()
    DATA["window"]["size"] = pg.display.get_window_size()
    DATA["journeymap"]["root"] = journeymap.path
    DATA["journeymap"]["world"] = journeymap.world

    file.write(json.dumps(DATA, indent=4))
    print("saved data.json")