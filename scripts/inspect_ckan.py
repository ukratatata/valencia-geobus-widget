import urllib.request
import json
import ssl

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

url = 'https://opendata.vlci.valencia.es/api/3/action/package_search?q=emt'
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
with urllib.request.urlopen(req, context=ctx, timeout=10) as r:
    data = json.loads(r.read().decode('utf-8'))
    results = data['result']['results']
    print(f'Total Datasets encontrados: {len(results)}')
    for ds in results:
        print('=============================================')
        print('Name:', ds.get('name'))
        print('Title:', ds.get('title'))
        print('Resources:')
        for res in ds.get('resources', []):
            print(f"  * [{res.get('format')}] {res.get('name')}: {res.get('url')}")
