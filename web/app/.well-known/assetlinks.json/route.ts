import { NextResponse } from "next/server";

export const dynamic = "force-static";

const assetLinksPayload = [
  {
    relation: [
      "delegate_permission/common.handle_all_urls",
      "delegate_permission/common.get_login_creds"
    ],
    target: {
      namespace: "android_app",
      package_name: "com.dutype.app",
      sha256_cert_fingerprints: [
        "01:34:37:6B:09:DB:60:01:0B:EA:45:6A:8F:AE:0A:02:29:C0:6D:98:FA:94:EC:B6:F3:13:6D:EE:C7:35:BC:B0",
        "84:B3:A3:10:BA:35:6A:6A:1F:C1:88:1D:5F:68:69:73:B2:3D:40:4E:D9:A1:00:11:B2:98:66:BA:95:B7:46:53",
        "55:68:54:C5:A8:AB:45:F8:EB:F0:CB:78:4F:B7:8B:7A:D1:B7:6E:90:05:1D:8A:AD:5C:07:E5:19:0D:55:FC:A2"
      ]
    }
  }
];

export function GET() {
  return NextResponse.json(assetLinksPayload, {
    headers: {
      "Cache-Control": "public, max-age=3600",
      "Access-Control-Allow-Origin": "*"
    }
  });
}
