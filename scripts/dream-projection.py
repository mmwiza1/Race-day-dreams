#!/usr/bin/env python3
"""Original WGS84 UTM18 conversion used only for frozen NYC context preparation.

Standard transverse Mercator series; no geographic library/runtime dependency.
The source course is NAD83 UTM18: metre-scale datum uncertainty is acceptable for
this simplified decorative overview, never for navigation or recorded metrics.
"""
import math

A = 6378137.0
E = 0.0066943799901413165
K = 0.9996


def course_frame(route):
    """Raw course coordinates to the unchanged 0–10 north-up drawing frame."""
    points = route['geometry']
    if 'frame' in route:
        xs, ys = [p[0] for p in points], [-p[1] for p in points]
        raw = lambda a, b: (a, -b)
    else:
        cosine = math.cos(math.radians(sum(p[0] for p in points)/len(points)))
        xs, ys = [p[1]*cosine for p in points], [-p[0] for p in points]
        raw = lambda a, b: (b*cosine, -a)
    scale = 9/max(max(xs)-min(xs), max(ys)-min(ys))
    xoff = (10-(max(xs)-min(xs))*scale)/2
    yoff = (10-(max(ys)-min(ys))*scale)/2
    def project(a, b):
        x, y = raw(a, b)
        return (x-min(xs))*scale+xoff, (y-min(ys))*scale+yoff
    return project


def geographic_frame(route):
    frame = course_frame(route)
    if 'frame' not in route:
        return frame
    anchor = route['frame']['anchor']
    def project(lat, lon):
        x, y = utm18(lat, lon)
        return frame(x-anchor[0], y-anchor[1])
    return project


def inverse_course_frame(route, x, y):
    """Invert the course's affine drawing frame, then the NYC UTM projection."""
    frame = course_frame(route)
    ox, oy = frame(0, 0)
    ax, ay = frame(1, 0)
    bx, by = frame(0, 1)
    ax, ay, bx, by = ax-ox, ay-oy, bx-ox, by-oy
    determinant = ax*by-ay*bx
    a = ((x-ox)*by-(y-oy)*bx)/determinant
    b = (ax*(y-oy)-ay*(x-ox))/determinant
    if 'frame' in route:
        anchor = route['frame']['anchor']
        return inverse18(a+anchor[0], b+anchor[1])
    return a, b


def utm18(lat, lon):
    lat, dl = math.radians(lat), math.radians(lon + 75)
    ep = E / (1 - E)
    n = A / math.sqrt(1-E*math.sin(lat)**2)
    t, c, aa = math.tan(lat)**2, ep*math.cos(lat)**2, math.cos(lat)*dl
    m = A*((1-E/4-3*E**2/64-5*E**3/256)*lat
           -(3*E/8+3*E**2/32+45*E**3/1024)*math.sin(2*lat)
           +(15*E**2/256+45*E**3/1024)*math.sin(4*lat)
           -35*E**3/3072*math.sin(6*lat))
    return (500000+K*n*(aa+(1-t+c)*aa**3/6+(5-18*t+t*t+72*c-58*ep)*aa**5/120),
            K*(m+n*math.tan(lat)*(aa*aa/2+(5-t+9*c+4*c*c)*aa**4/24
                                  +(61-58*t+t*t+600*c-330*ep)*aa**6/720)))


def inverse18(x, y):
    ep = E/(1-E)
    mu = y/K/(A*(1-E/4-3*E**2/64-5*E**3/256))
    e1 = (1-math.sqrt(1-E))/(1+math.sqrt(1-E))
    f = (mu+(3*e1/2-27*e1**3/32)*math.sin(2*mu)
         +(21*e1**2/16-55*e1**4/32)*math.sin(4*mu)
         +151*e1**3/96*math.sin(6*mu)+1097*e1**4/512*math.sin(8*mu))
    n = A/math.sqrt(1-E*math.sin(f)**2)
    r = A*(1-E)/(1-E*math.sin(f)**2)**1.5
    t, c, d = math.tan(f)**2, ep*math.cos(f)**2, (x-500000)/(n*K)
    lat = f-n*math.tan(f)/r*(d*d/2-(5+3*t+10*c-4*c*c-9*ep)*d**4/24
                               +(61+90*t+298*c+45*t*t-252*ep-3*c*c)*d**6/720)
    lon = (d-(1+2*t+c)*d**3/6+(5-2*c+28*t-3*c*c+8*ep+24*t*t)*d**5/120)/math.cos(f)
    return math.degrees(lat), -75+math.degrees(lon)
