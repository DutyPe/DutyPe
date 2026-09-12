
import json, os, urllib.request, random, string, time
from datetime import datetime, timezone

config_path = os.path.expanduser('~/.config/configstore/firebase-tools.json')
with open(config_path, 'r', encoding='utf-8') as cf:
    config = json.load(cf)

access_token = config['tokens']['access_token']
project_id = 'dutype-860ac'

BASE32 = '0123456789bcdefghjkmnpqrstuvwxyz'

def encode_geohash(lat, lng, precision=6):
    lat_range, lng_range = [-90.0, 90.0], [-180.0, 180.0]
    geohash = []
    bits = [16, 8, 4, 2, 1]
    bit = 0
    ch = 0
    even = True
    while len(geohash) < precision:
        if even:
            mid = (lng_range[0] + lng_range[1]) / 2
            if lng >= mid:
                ch |= bits[bit]
                lng_range[0] = mid
            else:
                lng_range[1] = mid
        else:
            mid = (lat_range[0] + lat_range[1]) / 2
            if lat >= mid:
                ch |= bits[bit]
                lat_range[0] = mid
            else:
                lat_range[1] = mid
        even = not even
        if bit < 4:
            bit += 1
        else:
            geohash.append(BASE32[ch])
            bit = 0
            ch = 0
    return ''.join(geohash)

DE_CHARS = string.ascii_letters + string.digits
def generate_doc_id():
    return ''.join(random.choice(DE_CHARS) for _ in range(20))

FIELD_KEYSD = {
    'nullValue': None,
    'booleanValue': None
}

def to_firestore_value(val):
    if val is None:
        return 'nullValue', None
    if isinstance(val, bool):
        return 'booleanValue', val
    if isinstance(val, int):
        return 'integerValue', str(val)
    if isinstance(val, float):
        return 'doubleValue', val
    if isinstance(val, str):
        return 'stringValue', val
    if isinstance(val, list):
        return 'arrayValue', {'values': [{k[0]: k[1]} for k in [to_firestore_value(x) for x in val]]}
    if isinstance(val, dict):
        if '_timestamp' in val:
            dt = datetime.fromtimestamp(val['_timestamp'] / 1000.0, tz=timezone.utc)
            return 'timestampValue', dt.isoformat()
        fs = {}
        for k, v in val.items():
            k_field, v_field = to_firestore_value(v)
            fs[k] = {k_field: v_field}
        return 'mapValue', {'fields': fs}
    return 'stringValue', str(val)

def write_doc(token, collection, doc_id, data):
    url = f'https://firestore.googleapis.com/v1/projects/{project_id}/databases/(default)/documents/{collection}/{doc_id}'
    fields = {}
    for k, v in data.items():
        k_field, v_field = to_firestore_value(v)
        fields[k] = {k_field: v_field}
    body = json.dumps({'fields': fields}).encode('utf-8')
    req = urllib.request.Request(url, data=body, method='PATCH', headers={
        'Authorization': f'Bearer {token}',
        'Content-Type': 'application/json'
    })
    with urllib.request.urlopen(req) as response:
        return json.loads(response.read().decode('utf-8'))

import sys
input_file = sys.argv[1] if len(sys.argv) > 1 else 'scripts/data/batch_jobs.json'
with open(input_file, 'r', encoding='utf-8') as jf:
    jobs = json.load(jf)

print(f'Starting bulk upload of {len(jobs)} jobs to Firestore...')

success = 0
failed = 0

for i, j in enumerate(jobs):
    doc_id = generate_doc_id()
    now_ms = int(time.time() * 1000) - (i * 1000)
    exp_ms = now_ms + (30 * 24 * 60 * 60 * 1000)
    lat = float(j.get('lat') or 17.3850)
    lng = float(j.get('lng') or 78.4867)
    geohash = encode_geohash(lat, lng, 6)
    vacancies = int(j.get('vacancies') or 1)
    
    card = {
        'jobId': doc_id,
        'employerId': 'admin1',
        'title': str(j.get('title', '')).strip(),
        'companyName': str(j.get('companyName', '')).strip(),
        'salary': str(j.get('salary', 'Negotiable')).strip(),
        'salaryType': str(j.get('salaryType', 'MONTHLY')).upper().strip(),
        'jobType': str(j.get('jobType', 'FULL_TIME')).upper().strip(),
        'category': str(j.get('category', 'Other')).strip(),
        'location': {'lat': lat, 'lng': lng},
        'geohash': geohash,
        'addressText': str(j.get('addressText', '')).strip(),
        'companyCity': str(j.get('city', 'Hyderabad')).strip(),
        'vacancies': vacancies,
        'status': 'open',
        'isVerified': True,
        'createdAt': {'_timestamp': now_ms},
        'expiresAt': {'_timestamp': exp_ms}
    }
    
    details = {
        'jobId': doc_id,
        'employerId': 'admin1',
        'title': card['title'],
        'companyName': card['companyName'],
        'salary': card['salary'],
        'salaryType': card['salaryType'],
        'jobType': card['jobType'],
        'category': card['category'],
        'location': card['location'],
        'geohash': geohash,
        'addressText': card['addressText'],
        'companyCity': card['companyCity'],
        'vacancies': vacancies,
        'contactNumber': ''.join(filter(str.isdigit, str(j.get('contactNumber', ''))))[-10:],
        'description': str(j.get('description', '')).strip(),
        'gender': str(j.get('gender', 'Any')).strip(),
        'experienceRequired': str(j.get('experienceRequired', 'Not Specified')).strip(),
        'educationRequired': str(j.get('educationRequired', 'No qualification required')).strip(),
        'shiftTiming': str(j.get('shiftTiming', 'Flexible')).strip(),
        'applicationCount': 0,
        'status': 'open',
        'isVerified': True,
        'createdAt': {'_timestamp': now_ms},
        'expiresAt': {'_timestamp': exp_ms}
    }
    
    try:
        write_doc(access_token, 'jobmetadata', doc_id, card)
        write_doc(access_token, 'jobs', doc_id, card)
        write_doc(access_token, 'job_details', doc_id, details)
        success += 1
        print(f'[{i+1}/{len(jobs)}] SUCCESS: {card["title"]} ({card["salary"]}) in {card["companyCity"]} -> ID: {doc_id}')
    except Exception as e:
        failed += 1
        print(f'[{i+1}/{len(jobs)}] FAILED: {card["title"]} -> {e}')

print(f'BULK UPLOAD SUMMARY: {success} POSTED SUCCESSFULLY, {failed} FAILED.')
