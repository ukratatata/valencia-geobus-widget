import urllib.request
import json
import ssl

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

# 1. Test count
url_count = 'https://geoportal.valencia.es/server/rest/services/OPENDATA/Trafico/MapServer/226/query?where=1=1&returnCountOnly=true&f=json'
req = urllib.request.Request(url_count, headers={'User-Agent': 'Mozilla/5.0'})
with urllib.request.urlopen(req, context=ctx, timeout=10) as r:
    data = json.loads(r.read().decode('utf-8'))
    print('Total paradas en la red EMT Valencia:', data)

# 2. Test sample query with outSR=4326
url_sample = 'https://geoportal.valencia.es/server/rest/services/OPENDATA/Trafico/MapServer/226/query?where=1=1&resultRecordCount=5&outFields=id_parada,denominacion,lineas,suprimida&outSR=4326&f=geojson'
req2 = urllib.request.Request(url_sample, headers={'User-Agent': 'Mozilla/5.0'})
with urllib.request.urlopen(req2, context=ctx, timeout=10) as r:
    data2 = json.loads(r.read().decode('utf-8'))
    features = data2.get('features', [])
    print(f'Muestra de {len(features)} paradas:')
    for f in features:
        props = f.get('properties', {})
        coords = f.get('geometry', {}).get('coordinates', [])
        print(f"  ID: {props.get('id_parada')} | Nombre: {props.get('denominacion')} | Lineas: {props.get('lineas')} | Coords (lon, lat): {coords}")

# 3. Test Max Record Count allowed by the server in one batch
url_max = 'https://geoportal.valencia.es/server/rest/services/OPENDATA/Trafico/MapServer/226?f=json'
req3 = urllib.request.Request(url_max, headers={'User-Agent': 'Mozilla/5.0'})
with urllib.request.urlopen(req3, context=ctx, timeout=10) as r:
    meta = json.loads(r.read().decode('utf-8'))
    print('MaxRecordCount soportado por el servidor:', meta.get('maxRecordCount'))
