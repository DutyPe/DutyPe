const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const { createRequire } = require("node:module");
const vm = require("node:vm");
const ts = require("typescript");

const root = path.resolve(__dirname, "..");
const modules = new Map();

function loadSource(relativePath, overrides, cache = overrides ? new Map() : modules) {
  const candidate = path.resolve(root, relativePath);
  const filename = [candidate, `${candidate}.ts`, `${candidate}.tsx`].find(
    (entry) => fs.existsSync(entry) && fs.statSync(entry).isFile()
  );
  assert.ok(filename, `Source module not found: ${relativePath}`);
  if (cache.has(filename)) return cache.get(filename).exports;

  const compiled = ts.transpileModule(fs.readFileSync(filename, "utf8"), {
    fileName: filename,
    compilerOptions: {
      module: ts.ModuleKind.CommonJS,
      target: ts.ScriptTarget.ES2020,
      jsx: ts.JsxEmit.ReactJSX,
      esModuleInterop: true
    },
    reportDiagnostics: true
  });
  assert.equal(compiled.diagnostics.filter((item) => item.category === ts.DiagnosticCategory.Error).length, 0);

  const loaded = { exports: {} };
  cache.set(filename, loaded);
  const nativeRequire = createRequire(filename);
  const localRequire = (specifier) => {
    if (overrides && Object.hasOwn(overrides, specifier)) return overrides[specifier];
    if (specifier.startsWith("@/")) return loadSource(specifier.slice(2), overrides, cache);
    if (specifier.startsWith(".")) return loadSource(path.resolve(path.dirname(filename), specifier), overrides, cache);
    return nativeRequire(specifier);
  };
  const execute = vm.runInThisContext(
    `(function(require, module, exports) {\n${compiled.outputText}\n})`,
    { filename }
  );
  execute(localRequire, loaded, loaded.exports);
  return loaded.exports;
}

module.exports = { loadSource };