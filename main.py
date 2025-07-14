import os, json
import pygame_gui as gui

from json import JSONDecodeError
from journeymap import *


DEFAULT_WINDOW_WIDTH = 800
DEFAULT_WINDOW_HEIGHT = 500
TOOLBAR_HEIGHT = 30
REGION_SIZE = 512

BACKGROUND_COLOR = ( 50,  50,  50)
TOOLBAR_COLOR    = ( 25,  25,  25, 128)

if not os.path.exists("data.json"):
    open("data.json", "x").close()
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


jm: Journeymap | None = Journeymap("/Users/ashton/Library/Application Support/ModrinthApp/profiles/1.21.4 fabric/journeymap",
                                   "mp/wild~survival")
show_map = False
current_world: str|None = None
current_dimension = Dimension.OVERWORLD
current_maptype = MapType.DAY
cached_regions: dict[tuple[int, int], pg.Surface] = dict()

pan_x: float = 0.0
pan_y: float = 0.0
zoom: float = 0.5


# initialize window
pg.init()
screen = pg.display.set_mode((DEFAULT_WINDOW_WIDTH, DEFAULT_WINDOW_HEIGHT) if not DATA
                             else DATA["window"]["size"],
                             pg.RESIZABLE)
if DATA: pg.display.set_window_position(DATA["window"]["position"])
pg.display.set_caption("Journeymap Viewer")
icon = pg.image.load("./assets/icon.png")
pg.display.set_icon(icon)
ui_manager = gui.UIManager((DEFAULT_WINDOW_WIDTH, DEFAULT_WINDOW_HEIGHT))

running = True
clock = pg.time.Clock()
time_delta = 0
keys_pressed = set()


def draw_regions(viewable_rectangle: tuple[float,float,float,float]):
    region_tile_size = int(REGION_SIZE * zoom)

    # Get which regions are visible on the screen
    viewable_topLeft = (
        int(viewable_rectangle[0] // region_tile_size),
        int(viewable_rectangle[1] // region_tile_size)
    )
    viewable_bottomRight = (
        int(viewable_rectangle[2] // region_tile_size) + 1,
        int(viewable_rectangle[3] // region_tile_size) + 1
    )

    used_regions: list[tuple[int, int]] = list()
    for region_z in range(viewable_topLeft[1], viewable_bottomRight[1]):
        for region_x in range(viewable_topLeft[0], viewable_bottomRight[0]):
            try:
                scaled = cached_regions[(region_z, region_x)]
            except KeyError:
                try:
                    region = jm.get_region(current_dimension, current_maptype, (region_x, region_z))
                    scaled = pg.transform.scale(region, (region_tile_size, region_tile_size))
                    cached_regions[(region_z, region_x)] = scaled
                except FileNotFoundError: continue

            # Calculate the position on screen to draw this region
            draw_x = region_x * region_tile_size - viewable_rectangle[0]
            draw_y = region_z * region_tile_size - viewable_rectangle[1]

            screen.blit(scaled, (draw_x, draw_y))
            used_regions += (region_z, region_x)

    # clean up cache
    for k in list(cached_regions.keys()):
        if k not in used_regions:
            del cached_regions[k]

def draw_toolbar():
    pg.draw.rect(screen, TOOLBAR_COLOR, (0, 0, screen.get_width(), TOOLBAR_HEIGHT))


while running:
    for event in pg.event.get():
        if event.type == pg.QUIT:
            running = False
        elif event.type == pg.KEYDOWN:
            keys_pressed.add(event.key)
        elif event.type == pg.KEYUP:
            keys_pressed.remove(event.key)
        elif event.type == pg.MOUSEWHEEL:
            if event.y > 0:
                zoom *= abs(event.y/3) * zoom * 1.1
            elif event.y < 0:
                zoom *= abs(event.y/3) * zoom * 0.9

        ui_manager.process_events(event)

    mouse_x, mouse_y = pg.mouse.get_pos()

    # panning
    if pg.K_UP in keys_pressed:
        pan_y -= 16.0
    if pg.K_DOWN in keys_pressed:
        pan_y += 16.0
    if pg.K_LEFT in keys_pressed:
        pan_x -= 16.0
    if pg.K_RIGHT in keys_pressed:
        pan_x += 16.0

    # zooming
    if pg.K_EQUALS in keys_pressed:
        zoom *= 1.1
    if pg.K_MINUS in keys_pressed:
        zoom *= 0.9

    print(zoom)
    if zoom < 0.1: zoom = 0.1
    if zoom > 4: zoom = 4.0

    # (x1, y1, x2, y2)
    viewable_rectangle = (
        pan_x,
        pan_y,
        pan_x + screen.get_width(),
        pan_y + screen.get_height()
    )
    viewable_center = (
        (viewable_rectangle[0] + viewable_rectangle[2]) / 2,
        (viewable_rectangle[1] + viewable_rectangle[3]) / 2
    )

    screen.fill(BACKGROUND_COLOR)
    draw_regions(viewable_rectangle)
    draw_toolbar()

    ui_manager.draw_ui(screen)
    ui_manager.update(time_delta)
    pg.display.flip()
    time_delta = clock.tick(60) / 1000


with open("data.json", "w") as file:
    DATA["window"]["position"] = pg.display.get_window_position()
    DATA["window"]["size"] = pg.display.get_window_size()
    DATA["journeymap"]["root"] = jm.path
    DATA["journeymap"]["world"] = jm.world

    file.write(json.dumps(DATA, indent=4))
    print("saved data.json")