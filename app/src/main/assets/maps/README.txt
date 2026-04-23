Map asset folder structure

- world.svg
  Active compatibility path used by the current world-map feature and existing backups.

- world/world.svg
  Canonical copy of the world SVG for future map organization.

- uae/
  Reserved folder for UAE region maps such as dubai.svg, sharjah.svg, and future city/emirate assets.

Notes

- Keep source map files as SVG inside this maps folder.
- Add future regional files under maps/uae/.
- Do not remove maps/world.svg until all persisted data and import/export compatibility are migrated.
