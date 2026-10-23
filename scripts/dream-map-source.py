#!/usr/bin/env python3
"""Original development-only MVT reader for bounded ODbL geographic extracts.

No downloaded styles, images, renderer or runtime tile client is used by the app.
The protobuf wire format and MVT integer geometry commands are public formats.
"""
import math
import struct


def varint(data, pos):
    value = shift = 0
    while pos < len(data) and shift < 70:
        byte = data[pos]
        pos += 1
        value |= (byte & 127) << shift
        if byte < 128:
            return value, pos
        shift += 7
    raise ValueError('Truncated/oversized protobuf integer')


def fields(data):
    pos = 0
    while pos < len(data):
        key, pos = varint(data, pos)
        number, wire = key >> 3, key & 7
        if wire == 0:
            value, pos = varint(data, pos)
        elif wire == 2:
            length, pos = varint(data, pos)
            if length > len(data) - pos:
                raise ValueError('Truncated protobuf field')
            value, pos = data[pos:pos+length], pos+length
        elif wire in (1, 5):
            length = 8 if wire == 1 else 4
            if pos+length > len(data):
                raise ValueError('Truncated fixed field')
            value, pos = data[pos:pos+length], pos+length
        else:
            raise ValueError('Unsupported protobuf wire type')
        yield number, value


def packed(data):
    pos = 0
    while pos < len(data):
        value, pos = varint(data, pos)
        yield value


def value(data):
    for key, v in fields(data):
        if key == 1:
            return v.decode('utf-8')
        if key == 2:
            return struct.unpack('<f', v)[0]
        if key == 3:
            return struct.unpack('<d', v)[0]
        if key in (4, 5, 7):
            return v
        if key == 6:
            return (v >> 1) ^ -(v & 1)
    return None


def geometry(data):
    commands = iter(packed(data))
    x = y = 0
    paths, current = [], []
    for command in commands:
        kind, count = command & 7, command >> 3
        if kind in (1, 2):
            for _ in range(count):
                dx, dy = next(commands), next(commands)
                x += (dx >> 1) ^ -(dx & 1)
                y += (dy >> 1) ^ -(dy & 1)
                if kind == 1 and current:
                    paths.append(current)
                    current = []
                current.append((x, y))
        elif kind == 7 and count == 1:
            if current and current[-1] != current[0]:
                current.append(current[0])
        else:
            raise ValueError('Invalid MVT geometry command')
    if current:
        paths.append(current)
    return paths


def layers(data):
    if len(data) > 5_000_000:
        raise ValueError('Oversized tile')
    for number, layer in fields(data):
        if number != 3:
            continue
        contents = list(fields(layer))
        name = next(v.decode() for k, v in contents if k == 1)
        keys = [v.decode() for k, v in contents if k == 3]
        values = [value(v) for k, v in contents if k == 4]
        extent = next((v for k, v in contents if k == 5), 4096)
        features = []
        for k, raw in contents:
            if k != 2:
                continue
            f = dict(fields(raw))
            tags = list(packed(f.get(2, b'')))
            properties = {keys[tags[i]]: values[tags[i+1]] for i in range(0, len(tags), 2)}
            features.append((properties, f.get(3), geometry(f.get(4, b''))))
        yield name, extent, features


def lonlat(x, y, zoom, extent, point):
    scale = 2 ** zoom
    lon = (x+point[0]/extent)/scale*360-180
    lat = math.degrees(math.atan(math.sinh(math.pi*(1-2*(y+point[1]/extent)/scale))))
    return [round(lat, 7), round(lon, 7)]
