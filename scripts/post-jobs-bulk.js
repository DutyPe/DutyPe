'use strict';

const fs = require('fs');
const path = require('path');
const os = require('os');
const https = require('https');

const PROJECT_ID = 'dutype-860ac';
const BASE32 = '0123456789bcdefghjkmnpqrstuvwxyz';

function encodeGeohash(lat, lng, precision = 6) {
  let latRange = [-90, 90], lngRange = [-180, 180];
  let hash = '', bits = 0, ch = 0, isLng = true;
  while (hash.length < precision) {
    const range = isLng ? lngRange : latRange;
    const mid = (range[0] + range[1]) / 2;
    const val = isLng ? lng : lat;
    if (val >= mid) { ch = ch * 2 + 1; range[0] = mid; }
    else            { ch = ch * 2;     range[1] = mid; }
    bits++;
    if (bits === 5) { hash += BASE32[ch]; bits = 0; ch = 0; }
    isLng = !isLng;
  }
  return hash;
}

function generateDocId() {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
  let autoId = '';
  for (let i = 0; i < 20; i++) {
    autoId += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return autoId;
}

async function getAccessToken() {
  const configPath = path.join(os.homedir(), '.config', 'configstore', 'firebase-tools.json');
  if (!fs.existsSync(configPath)) {
    throw new Error('firebase-tools.json not found. Run firebase login first.');
  }
  const config = JSON.parse(fs.readFileSync(configPath, 'utf8'));
  const tokens = config.tokens;

  if (tokens.access_token && tokens.expires_at && tokens.expires_at > Date.now() + 60000) {
    return tokens.access_token;
  }

  return new Promise((resolve, reject) => {
    const postData = new URLSearchParams({
      client_id: '563584335869-fgrhgmd47bqnekij5i8b5pr03ho85qd6.apps.googleusercontent.com',
      grant_type: 'refresh_token',
      refresh_token: tokens.refresh_token
    }).toString();

    const req = https.request('https://oauth2.googleapis.com/token', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
        'Content-Length': Buffer.byteLength(postData)
      }
    }, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        try {
          const json = JSON.parse(data);
          if (json.access_token) resolve(json.access_token);
          else reject(new Error('Failed to refresh token: ' + data));
        } catch (e) { reject(e); }
      });
    });
    req.on('error', reject);
    req.write(postData);
    req.end();
  });
}

function toFirestoreValue(val) {
  if (val === null || val === undefined) return { nullValue: null };
  if (typeof val === 'boolean') return { booleanValue: val };
  if (typeof val === 'number') {
    if (Number.isInteger(val)) return { integerValue: String(val) };
    return { doubleValue: val };
  }
  if (typeof val === 'string') return { stringValue: val };
  if (val instanceof Date) return { timestampValue: val.toISOString() };
  if (Array.isArray(val)) {
    return { arrayValue: { values: val.map(toFirestoreValue) } };
  }
  if (typeof val === 'object') {
    if (val._timestamp) return { timestampValue: new Date(val._timestamp).toISOString() };
    const fields = {};
    for (const [k, vWR�b�&�V7B�V�G&�W2�f���f�V�G5����F�f�&W7F�&Uf�VR�b���Т&WGW&���f�VS��f�V�G2�Ӱ�Т&WGW&��7G&��uf�VS�7G&��r�fӰ�Р�7��2gV�7F���w&�FTf�&W7F�&TF�7V�V�B�66W75F��V��6���V7F����F�4�B�FF���6��7Bf�V�G2��Ӱ�f�"�6��7B���e��b�&�V7B�V�G&�W2�FF����f�V�G5����F�f�&W7F�&Uf�VR�b���Р�6��7BW&��v�GG3���f�&W7F�&R�v��v�V�2�6���c�&��V7G2�r�$��T5E��B�r�FF&6W2�FVfV�B��F�7V�V�G2�r�6���V7F����r�r�F�4�C���&WGW&��Wr&�֗6R��&W6��fR�&V�V7B�����6��7B�7DFF��4���7G&��v�g���f�V�G2ғ��6��7B&W��GG2�&WVW7B�W&�����WF��C�t�D4�r���VFW'3���tWF��&��F���s�t&V&W"r�66W75F��V���t6��FV�B�G�Rs�vƖ6F�����6��r��t6��FV�B��V�wF�s�'VffW"�'�FT�V�wF���7DFF��Т���&W2������WBFF�rs��&W2���vFFr�6�V���FF��6�V沓��&W2���vV�Br��������b�&W2�7FGW46�FR��#bb&W2�7FGW46�FR�3���&W6��fR��4���'6R�FF�����V�6R��&V�V7B��WrW'&�"�tf�&W7F�&RW'&�"�r�&W2�7FGW46�FR�uӢr�FF����Тғ��ғ��&W���vW'&�"r�&V�V7B���&W�w&�FR��7DFF���&W�V�B����ғ��Р�7��2gV�7F����7D��"�66W75F��V��&t��"���FW�����6��7B��$�B�&t��"�B��&t��"��$�B��vV�W&FTF�4�B����6��7B��r�FFR���r������FW�����6��7BW��&W4D�6���WrFFR���r�3�#B�c�c�����6��7B�B��V�&W"�&t��"��B���r�3�S��6��7B��r��V�&W"�&t��"���r���s��C�cs��6��7BvV��6��V�6�FTvV��6���B���r�b���6��7Bf6�6�W2��V�&W"�&t��"�f6�6�W2�����6��7BV����W$�B�&t��"�V����W$�B��vF֖�s���6��7B6&DFF�����$�B��V����W$�B��F�F�S�7G&��r�&t��"�F�F�R��rr��G&�҂���6�����S�7G&��r�&t��"�6�����R��rr��G&�҂���6�'��7G&��r�&t��"�6�'���t�Vv�F�&�Rr��G&�҂���6�'�G�S�7G&��r�&t��"�6�'�G�R��t���D�Œr��WW&66R���G&�҂�����%G�S�7G&��r�&t��"��%G�R��teT���D��Rr��WW&66R���G&�҂���6FVv�'��7G&��r�&t��"�6FVv�'���t�F�W"r��G&�҂�����6F�����B���r���vV��6���FG&W75FW�C�7G&��r�&t��"�FG&W75FW�B��rr��G&�҂���6���6�G��7G&��r�&t��"�6�G���t��FW&&Br��G&�҂���f6�6�W2��7FGW3�v�V�r���5fW&�f�VC�G'VR��7&VFVDC���F��W7F����r���W��&W4C���F��W7F��W��&W4D�6��vWEF��R��ТӰ��6��7BFWF��4FF�����$�B��V����W$�B��F�F�S�6&DFF�F�F�R��6�����S�6&DFF�6�����R��6�'��6&DFF�6�'���6�'�G�S�6&DFF�6�'�G�R����%G�S�6&DFF��%G�R��6FVv�'��6&DFF�6FVv�'�����6F���6&DFF���6F�����vV��6���FG&W75FW�C�6&DFF�FG&W75FW�B��6���6�G��6&DFF�6���6�G���f6�6�W2��6��F7D�V�&W#�7G&��r�&t��"�6��F7D�V�&W"��rr��&W�6R���B�r�rr��6Ɩ6R�����FW67&�F���7G&��r�&t��"�FW67&�F�����rr��G&�҂���vV�FW#�7G&��r�&t��"�vV�FW"��t�r��G&�҂���W�W&�V�6U&WV�&VC�7G&��r�&t��"�W�W&�V�6U&WV�&VB��t��B7V6�f�VBr��G&�҂���VGV6F���&WV�&VC�7G&��r�&t��"�VGV6F���&WV�&VB��t��VƖf�6F���&WV�&VBr��G&�҂���6��gEF�֖�s�7G&��r�&t��"�6��gEF�֖�r��tf�W��&�Rr��G&�҂���Ɩ6F���6�V�C���7FGW3�v�V�r���5fW&�f�VC�G'VR��7&VFVDC���F��W7F����r���W��&W4C���F��W7F��W��&W4D�6��vWEF��R��ТӰ��v�B&�֗6R����w&�FTf�&W7F�&TF�7V�V�B�66W75F��V��v��&�WFFFr���$�B�6&DFF���w&�FTf�&W7F�&TF�7V�V�B�66W75F��V��v��'2r���$�B�6&DFF���w&�FTf�&W7F�&TF�7V�V�B�66W75F��V��v��%�FWF��2r���$�B�FWF��4FF��ғ���&WGW&����$�B�F�F�S�6&DFF�F�F�R�6��瓢6&DFF�6�����RӰ�Р�7��2gV�7F����7D��'4'VƲ���'4Ɨ7B���6��6��R���r�u7F'F��r'VƲW��B�br���'4Ɨ7B��V�wF��r��"�2����r���6��7B66W75F��V��v�BvWD66W75F��Vₓ���6��7B&W7V�G2��Ӱ�f�"��WB�������'4Ɨ7B��V�wF��������6��7B��"���'4Ɨ7E��Ӱ�G'���6��7B&W2�v�B�7D��"�66W75F��V����"�����6��6��R���r�~)�R�r������r�r���'4Ɨ7B��V�wF��u��7FVC�r�&W2�F�F�R�rBr�&W2�6����r��C�r�&W2��$�B�r�r���&W7V�G2�W6���7V66W73�G'VR����&W2ғ���6F6��W'"���6��6��R�W'&�"�~)���r������r�r���'4Ɨ7B��V�wF��u�f��VBF��7Br���"�F�F�R�s�r�W'"��W76vR���&W7V�G2�W6���7V66W73�f�6R�F�F�S���"�F�F�R�W'&�#�W'"��W76vRғ��ТР�6��6��R���r�u�7FVBr�&W7V�G2�f��FW"�"��"�7V66W72���V�wF��r�r���'4Ɨ7B��V�wF��r��'2�r���&WGW&�&W7V�G3��Р���GV�R�W��'G2���7D��'4'VƲ��7D��"�V�6�FTvV��6�Ӱ