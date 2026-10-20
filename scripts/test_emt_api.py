import urllib.request
import json
import ssl
import re
import time

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

def test_full_flow():
    print("================================================================================")
    print(" FASE 0: VERIFICACIÓN Y VALIDACIÓN DE PAYLOADS REALES (VALÈNCIA GEOBUS WIDGET)")
    print("================================================================================")
    
    # 1. CATALOG API: Geoportal OpenData Valencia (Ayuntamiento de València / EMT)
    print("\n[1] CATÁLOGO OFICIAL DE PARADAS (Geoportal OpenData València)")
    stop_id = "1694"
    catalog_url = f"https://geoportal.valencia.es/server/rest/services/OPENDATA/Trafico/MapServer/226/query?where=id_parada={stop_id}&outFields=id_parada,denominacion,lineas,suprimida&outSR=4326&f=geojson"
    print(f"URL: {catalog_url}")
    
    req_cat = urllib.request.Request(catalog_url, headers={'User-Agent': 'ValènciaGeoBus/1.0 (Android 16)'})
    with urllib.request.urlopen(req_cat, context=ctx, timeout=10) as r:
        cat_json = json.loads(r.read().decode('utf-8'))
        print("Payload GeoJSON real recibido:")
        print(json.dumps(cat_json, indent=2, ensure_ascii=False))
        
        feature = cat_json['features'][0]
        lon, lat = feature['geometry']['coordinates']
        props = feature['properties']
        print(f"\n-> Campos mapeados:")
        print(f"   * stopId: {int(props['id_parada'])}")
        print(f"   * stopName: {props['denominacion']}")
        print(f"   * lines: {props['lineas']}")
        print(f"   * latitude: {lat}")
        print(f"   * longitude: {lon}")

    # 2. REAL-TIME API: EMT Valencia
    print("\n--------------------------------------------------------------------------------")
    print("[2] TIEMPO REAL ESTIMADO (EMT València Web Service)")
    rt_url = f"https://www.emtvalencia.es/ciudadano/modules/mod_tiempo/busca_parada.php?parada={stop_id}&adaptados=0&idioma=es"
    print(f"URL: {rt_url}")
    
    req_rt = urllib.request.Request(rt_url, headers={
        'User-Agent': 'Mozilla/5.0 (Linux; Android 16; SM-S928B) AppleWebKit/537.36',
        'Referer': 'https://www.emtvalencia.es/'
    })
    with urllib.request.urlopen(req_rt, context=ctx, timeout=10) as r:
        rt_html = r.read().decode('utf-8', errors='ignore')
        print(f"Payload HTML real recibido (HTTP {r.status}):")
        print(rt_html.strip())
        
        pattern = re.compile(
            r'title="([^"]+)"[^>]*>.*?<span class="llegadaHome">\s*(&nbsp;)*\s*([^-<]+)-\s*([^<]+)</span>',
            re.DOTALL | re.IGNORECASE
        )
        matches = pattern.findall(rt_html)
        print("\n-> Extracción de llegadas:")
        arrivals_by_line = {}
        for m in matches:
            line_id = m[0].strip()
            destination = m[2].strip()
            time_str = m[3].strip()
            if line_id not in arrivals_by_line:
                arrivals_by_line[line_id] = []
            arrivals_by_line[line_id].append((destination, time_str))
            
        for line, arrivals in arrivals_by_line.items():
            first_bus = arrivals[0] if len(arrivals) > 0 else ("-", "-")
            second_bus = arrivals[1] if len(arrivals) > 1 else None
            second_str = f" • {second_bus[1]}" if second_bus else ""
            print(f"   * Línea {line} ({first_bus[0]}): {first_bus[1]}{second_str}")

    print("\n================================================================================")
    print(" FASE 0 COMPLETADA CON ÉXITO: CONTRATOS Y ESQUEMAS VALIDADOS")
    print("================================================================================")

if __name__ == "__main__":
    test_full_flow()
