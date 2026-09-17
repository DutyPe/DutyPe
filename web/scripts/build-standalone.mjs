import { cpSync, existsSync, mkdirSync, readFileSync } from "node:fs";
import { createRequire } from "node:module";
import path from "node:path";
import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const require = createRequire(import.meta.url);
const next = require.resolve("next/dist/bin/next");
const result = spawnSync(process.execPath, [next, "build"], {
  cwd: root,
  stdio: "inherit",
  env: { ...process.env, NEXT_STANDALONE: "1" }
});

if (result.error) throw result.error;
if (result.status !== 0) process.exit(result.status ?? 1);

const output = path.join(root, ".next", "standalone");
if (!existsSync(path.join(output, "server.js"))) throw new Error("Next.js did not generate the standalone server.");
mkdirSync(path.join(output, ".next"), { recursive: true });
cpSync(path.join(root, ".next", "static"), path.join(output, ".next", "static"), { recursive: true });
if (existsSync(path.join(root, "public"))) cpSync(path.join(root, "public"), path.join(output, "public"), { recursive: true });

const standaloneRequire = createRequire(path.join(output, "server.js"));
const loadedModules = new Set(Object.keys(standaloneRequire.cache));
const sharpEntry = standaloneRequire.resolve("sharp");
if (!sharpEntry.startsWith(`${output}${path.sep}`)) throw new Error("The standalone output is missing its sharp runtime dependency.");
const sharp = standaloneRequire("sharp");
const { optimizeImage } = standaloneRequire("next/dist/server/image-optimizer");
const optimizedImage = await optimizeImage({
  buffer: readFileSync(path.join(output, "public", "dutype-logo.webp")),
  contentType: "image/webp",
  quality: 75,
  width: 64,
  nextConfigOutput: "standalone"
});
const imageMetadata = await sharp(optimizedImage).metadata();
if (imageMetadata.format !== "webp" || imageMetadata.width !== 64 || !imageMetadata.height) {
  throw new Error("Standalone image optimization did not produce the expected WebP image.");
}
if (Object.keys(standaloneRequire.cache).some((file) => !loadedModules.has(file) && !file.startsWith(`${output}${path.sep}`))) {
  throw new Error("Standalone image optimization loaded a dependency outside the packaged output.");
}
console.log("Standalone native image optimization verified (64px WebP).");
console.log("Standalone server, static bundles and public assets are ready in web/.next/standalone.");