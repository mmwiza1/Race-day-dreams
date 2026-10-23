#!/usr/bin/env python3
"""Optional development source refresh, NEVER called by the app or CI.

Uses a frozen OpenFreeMap dataset with explicit bounded offline-download terms.
Retains ODbL data only, no map imagery/styles/software. Existing data are stable:
review generated data changes, update provenance, recapture/review all five cities.
"""
import argparse
import concurrent.futures, hashlib, json, math, pathlib, runpy, urllib.request
args=argparse.ArgumentParser(description=__doc__)
args.add_argument('--cache',type=pathlib.Path,required=True,help='Local source tile cache, outside the app')
args.add_argument('--fetch',action='store_true',help='Explicitly allow bounded network source preparation')
options=args.parse_args()
root=pathlib.Path(__file__).resolve().parent.parent
m=runpy.run_path(str(root/'scripts/dream-map-source.py'))
p=runpy.run_path(str(root/'scripts/dream-projection.py'))
cache=options.cache;cache.mkdir(parents=True,exist_ok=True)
files=['london-historical.json','berlin-2014-candidate.json','tokyo-2026.json','paris-2024-olympic.json','new-york-2026.json']
zoom=12
urlbase='https://tiles.openfreemap.org/planet/20261018_080001_pt'
plans=[];alltiles=set()
for file in files:
 route=json.loads((root/'docs/dream-route-data'/file).read_text());points=route['geometry']
 if route['id']=='berlin-2014-candidate':route['id']='berlin-2014-geo-v1'
 if 'frame' in route:
  anchor=route['frame']['anchor'];points=[p['inverse18'](a+anchor[0],b+anchor[1]) for a,b in points]
 south,north=min(a for a,b in points)-.012,max(a for a,b in points)+.012
 west,east=min(b for a,b in points)-.012,max(b for a,b in points)+.012
 def tile(lat,lon):return int((lon+180)/360*2**zoom),int((1-math.asinh(math.tan(math.radians(lat)))/math.pi)/2*2**zoom)
 xmin,ymin=tile(north,west);xmax,ymax=tile(south,east)
 tiles=[(x,y) for x in range(xmin,xmax+1) for y in range(ymin,ymax+1)]
 plans.append((route,tiles,[south,west,north,east]));alltiles.update(tiles)
assert len(alltiles)<100
print('Bounded source preparation',len(alltiles),'tiles',flush=True)
def fetch(t):
 x,y=t;url=f'{urlbase}/{zoom}/{x}/{y}.pbf';path=cache/f'{zoom}-{x}-{y}.pbf'
 if not path.exists():
  if not options.fetch:raise RuntimeError('Missing cached source; use --fetch only for intentional development refresh')
  req=urllib.request.Request(url,headers={'User-Agent':'RaceDayDreams-development-geographic-extract/0.8.1 (https://github.com/mmwiza1/Race-day-dreams)'})
  with urllib.request.urlopen(req,timeout=35) as r: data=r.read(5_000_001)
  assert len(data)<5_000_000;path.write_bytes(data)
 return path.stat().st_size
with concurrent.futures.ThreadPoolExecutor(max_workers=3) as pool:sizes=list(pool.map(fetch,sorted(alltiles)))
assert sum(sizes)<50_000_000
print('Cached source bytes',sum(sizes),flush=True)
def simplify(points,epsilon):
 if len(points)<3:return points
 a,b=points[0],points[-1];dx=b[1]-a[1];dy=b[0]-a[0];den=dx*dx+dy*dy
 maxd,index=0,0
 for i,q in enumerate(points[1:-1],1):
  t=0 if den==0 else max(0,min(1,((q[1]-a[1])*dx+(q[0]-a[0])*dy)/den))
  d=(q[1]-a[1]-t*dx)**2+(q[0]-a[0]-t*dy)**2
  if d>maxd:maxd,index=d,i
 if maxd<=epsilon**2:return [a,b]
 return simplify(points[:index+1],epsilon)[:-1]+simplify(points[index:],epsilon)
for route,tiles,bounds in plans:
 features=[];seen=set();tile_sources=[]
 south,west,north,east=bounds
 for x,y in tiles:
  raw=(cache/f'{zoom}-{x}-{y}.pbf').read_bytes()
  tile_sources.append({'url':f'{urlbase}/{zoom}/{x}/{y}.pbf','sha256':hashlib.sha256(raw).hexdigest(),'bytes':len(raw)})
  for layer,extent,items in m['layers'](raw):
   if layer not in ('water','landcover','park','transportation'):continue
   for props,kind,paths in items:
    cls=props.get('class','')
    if layer=='transportation':
     if kind!=2 or cls not in ('motorway','trunk','primary','secondary','tertiary','minor'):continue
     category='road'
    elif layer=='water':
     if kind!=3 or props.get('intermittent',0):continue
     category='water'
    else:
     if kind!=3 or (layer=='landcover' and cls not in ('wood','grass')):continue
     category='green'
    retained=[]
    for path in paths:
     ll=[m['lonlat'](x,y,zoom,extent,xy) for xy in path]
     if len(ll)<2 or max(a for a,b in ll)<south or min(a for a,b in ll)>north or max(b for a,b in ll)<west or min(b for a,b in ll)>east:continue
     ll=simplify(ll,.00007)
     if kind==3 and len(ll)<4:continue
     if kind==2 and sum(math.hypot((b[0]-a[0]),(b[1]-a[1])*.65) for a,b in zip(ll,ll[1:]))<.0011:continue
     retained.append(ll)
    if not retained:continue
    key=json.dumps([category,retained],separators=(',',':'))
    if key in seen:continue
    seen.add(key);features.append({'kind':category,'paths':retained})
 out={'format':1,'routeId':route['id'],'checked':'2026-10-23','dataset':'OpenFreeMap planet20261018_080001_pt, OpenMapTiles schema, OpenStreetMap data','attribution':'OpenFreeMap © OpenMapTiles · © OpenStreetMap contributors · ODbL','licence':'ODbL-1.0','licenceUrl':'https://opendatacommons.org/licenses/odbl/1-0/','usagePolicy':'https://openfreemap.org/tos/','sourceBounds':bounds,'sourceTiles':tile_sources,'transformation':'Only selected water/green-space polygons and major/minor road lines retained. Original MVT data decoded using original format reader. RDP simplification retains source positions at0.00007degree tolerance; no invented streets, labels, styles, imagery or tile artwork. Polygons clipped by source tiles may include holes. Road fragments shorter than approximately120m omitted for overview legibility. Frame projection is performed separately. NYC WGS84-to-NAD83 approximation has metre-scale datum uncertainty; decorative overview only.','features':features}
 target=root/'docs/dream-route-data'/('basemap-'+route['id']+'.json')
 target.write_text(json.dumps(out,separators=(',',':'))+'\n')
 print(route['id'],len(features),'features',sum(len(q) for f in features for q in f['paths']),'positions',target.stat().st_size,'bytes',flush=True)
