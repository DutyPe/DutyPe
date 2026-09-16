# Pretext

`chenglou-pretext-0.0.9.tgz` contains the official `@chenglou/pretext` source built locally because npm's registry is blocked in this environment. The library has no runtime dependencies. Its original MIT license and package metadata are included in the archive; no upstream source was modified.

- Source: https://github.com/chenglou/pretext
- Revision: `12b72a069b77402ac50448452c48995dbd2676e4`
- Version: `0.0.9`
- Compiler: TypeScript `5.9.3`
- Build: `tsc -p tsconfig.build.json`
- Package: `npm pack <source-directory> --ignore-scripts --offline`
- SHA-512 integrity: `sha512-LdNjQ22FdjTEIp3vKBsRTdrDjYUpzU328SBrTYt69B7vVnHk8D7j6moji7rSXuVB6beD/iGuM+OpKTHdR1nv+A==`

The archive is installed through a relative `file:vendor/` dependency, so it does not depend on a developer's temporary directory. It is a build of the pinned official source, not a claim of byte-for-byte equivalence with the npm release. Once registry access is available, the dependency can be replaced by the official published package after rerunning the browser tests.

## Server-Only Marker

`server-only-0.0.1.tgz` is packaged without modification from `next@14.2.35/dist/compiled/server-only`. Its package metadata declares the MIT license. The marker's `react-server` export and default client-import error are unchanged. It is included locally because the registry package was not cached and registry access was blocked.

- Build: none
- Package: `npm pack ./node_modules/next/dist/compiled/server-only --offline --ignore-scripts --pack-destination vendor`
- SHA-512 integrity: `sha512-v3t9+kqxpQspq14uq8K1zvCL4nsTGUlaqraBjMenTH4oYEe46ngYYgr0Pl/yz5uo44KFHMYQagtKPpXQRUQYkw==`

The exact ambient declaration in `types/server-only.d.ts` describes this side-effect-only marker. It does not remove or replace the runtime import guard.