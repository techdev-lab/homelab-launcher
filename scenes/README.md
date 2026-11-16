# Scenes pictures

`master/` holds the five scene pictures exactly as published by couch-tv (MIT, `Copyright (c) 2026 Seagey`; see
[NOTICE.md](../NOTICE.md)). `python scenes/to-scenes.py` writes `<id>/bg.webp`, `glass.webp` and `cut.webp` and
`manifest.json`; only the `.webp` files go into the APK. `python scenes/to-scenes.py --check` verifies everything and
changes nothing. Design: [docs/BATCH_S_SCENES.md](../docs/BATCH_S_SCENES.md).
