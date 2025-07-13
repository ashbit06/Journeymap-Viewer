import os, re
import pygame as pg
from enum import Enum


class Dimension(Enum):
    OVERWORLD = "overworld"
    NETHER = "the_nether"
    END = "the_end"


class MapType(Enum):
    CAVE = 0
    BIOME = "biome"
    DAY = "day"
    NIGHT = "night"
    TOPO = "topo"


class Journeymap:
    def __init__(self, path: str, world: str):
        """
        journeymap shit

        :param path: path to the minecraft folder
        :param world: path to the world's journeymap folder
        """
        if not os.path.exists(path):
            raise FileNotFoundError
        if not os.path.isdir(path):
            raise NotADirectoryError
        if os.path.split(path)[-1] != "journeymap":
            raise NameError("You must select the Journeymap directory form your Minecraft folder")
        if not all([f in os.listdir(path) for f in os.listdir(path)]):
            raise Exception

        self.__path = path
        self.world = world

    @property
    def path(self):
        return self.__path

    @property
    def world(self):
        return self.__world

    @world.setter
    def world(self, value: str):
        p = re.search("^/?([ms]p/[^/]+)/?$", value)
        if not p:
            raise ValueError("world path needs to be in the format of mp/[world name] or sp/[world name]")
        if not os.path.exists(f"{self.path}/data/{p.group(1)}"):
            raise FileNotFoundError

        self.__world = value

    def get_path(self) -> str:
        return f"{self.path}/data/{self.world}"

    def get_dimensions(self) -> list[str]:
        return [i for i in os.listdir(self.get_path()) if i != "waypoints"]

    def get_region(self, dimension: Dimension, map_type: MapType, coordinates: tuple[int, int]|tuple[int, int, int]) -> pg.Surface:
        map_path = f"{self.get_path()}/{dimension.value}/"

        if map_type == MapType.CAVE:
            if len(coordinates) != 3:
                raise ValueError("when using the cave map, the coordinates parameter must have 3 values (x, z, vertical chunk)")
            map_path += f"{coordinates[2]}/{coordinates[0]},{coordinates[1]}.png"
        else:
            if len(coordinates) != 2:
                raise ValueError("when using any map that isnt the cave map, the coordinates parameter must have 2 values (x, z)")
            map_path += f"{map_type.value}/{coordinates[0]},{coordinates[1]}.png"

        return pg.image.load(map_path)


if __name__ == '__main__':
    jm = Journeymap("~/Library/Application Support/ModrinthApp/profiles/1.21.4 fabric/journeymap",
                    "mp/wild~survival")