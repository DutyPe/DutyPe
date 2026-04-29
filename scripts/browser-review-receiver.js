'use strict';

const fs = require('fs');
const http = require('http');
const path = require('path');

function parseArgs(argv) {
  const args = {
    out: path.join(__dirname, 'output', 'browser-review-payload.json'),
    port: 4319,
    once: false
  };

  for (let index = 0; index < argv.length; index += 1) {
    const token = argv[index];
    const next = () => {
      index += 1;
      if (index >= argv.length) throw new Error(`Missing value after ${token}`);
      return argv[index];
    };

    switch (token) {
      case '--out':
        args.out = next();
        break;
      case '--port':
        args.port = Number.parseInt(next(), 10);
        break;
      case '--once':
        args.once = true;
        break;
      default:
        throw new Error(`Unknown option: ${token}`);
    }
  }

  if (!Number.isInteger(args.port) || args.port < 1 || args.port > 65535) {
    throw new Error('--port must be a valid TCP port');
  }

  args.out = path.isAbsolute(args.out) ? args.out : path.resolve(process.cwd(), args.out);
  return args;
}

function setCors(response) {
  response.setHeader('Access-Control-Allow-Origin', '*');
  response.setHeader('Access-Control-Allow-Methods', 'GET,POST,OPTIONS');
  response.setHeader('Access-Control-Allow-Headers', 'Content-Type');
}

function main() {
  const args = parseArgs(process.argv.slice(2));
  const server = http.createServer((request, response) => {
    setCors(response);
    if (request.method === 'OPTIONS') {
      response.writeHead(204);
      response.end();
      return;
    }

    if (request.method === 'GET') {
      response.writeHead(200, { 'Content-Type': 'application/json' });
      response.end(JSON.stringify({ ok: true, out: args.out }));
      return;
    }

    if (request.method !== 'POST') {
      response.writeHead(405, { 'Content-Type': 'application/json' });
      response.end(JSON.stringify({ ok: false, error: 'methodNotAllowed' }));
      return;
    }

    const chunks = [];
    let totalBytes = 0;
    request.on('data', (chunk) => {
      totalBytes += chunk.length;
      if (totalBytes > 50 * 1024 * 1024) {
        request.destroy(new Error('Payload too large'));
        return;
      }
      chunks.push(chunk);
    });
    request.on('end', () => {
      const body = Buffer.concat(chunks).toString('utf8');
      JSON.parse(body);
      fs.mkdirSync(path.dirname(args.out), { recursive: true });
      fs.writeFileSync(args.out, `${body}\n`, 'utf8');
      console.log(`Wrote ${totalBytes} bytes to ${args.out}`);
      response.writeHead(200, { 'Content-Type': 'application/json' });
      response.end(JSON.stringify({ ok: true, bytes: totalBytes, out: args.out }));
      if (args.once) {
        setTimeout(() => server.close(() => process.exit(0)), 100);
      }
    });
    request.on('error', (error) => {
      response.writeHead(500, { 'Content-Type': 'application/json' });
      response.end(JSON.stringify({ ok: false, error: error.message }));
    });
  });

  server.listen(args.port, '127.0.0.1', () => {
    console.log(`Listening on http://127.0.0.1:${args.port} -> ${args.out}`);
  });
}

main();