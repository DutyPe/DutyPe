# DutyPe Service Images Directory

Drop your generated 3D isometric clay render images here.

## Directory Structure
```
scripts/images/
├── categories/
│   ├── ac.webp
│   ├── cleaning.webp
│   ├── electrician.webp
│   ├── plumber.webp
│   ├── appliance.webp
│   ├── carpenter.webp
│   ├── painter.webp
│   ├── home_help.webp
│   └── vehicle.webp
└── services/
    ├── clean_sweep.webp
    ├── clean_bathroom.webp
    ├── clean_kitchen.webp
    ├── clean_1bhk.webp
    ├── ac_service.webp
    ├── ac_gas.webp
    ├── elec_fan_install.webp
    ├── plumb_tap.webp
    ├── app_ro_service.webp
    └── ... (see docs/SERVICE_IMAGE_PROMPTS.md for full list of IDs)
```

## Recommended Specs (Urban Company / Zepto Standard):
- **Format**: WebP (preferred for small file size ~30KB) or PNG with transparent background.
- **Resolution**: 512×512 px (or 256×256 px).
- **Background**: Transparent.
- **Style**: Cute 3D isometric clay render, pastel matte finish, soft studio lighting.

## Uploading:
Run either:
```bash
node scripts/upload_service_images.mjs
```
or
```bash
python scripts/upload_service_images.py
```
Or drag-and-drop the files directly in Firebase Console > Storage > `services/` and `categories/`.
