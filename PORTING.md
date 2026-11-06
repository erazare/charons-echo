# Porting note — NeoForge 26.3

**Charon's Echo is the original work of BeachFury (Sid), MIT-licensed.
The design, the writing, and the substance of the code are his.
This file exists so that never gets muddy.**

- `master` in this fork is Sid's original Fabric source, untouched.
- This branch, `neoforge-26.3-port`, is an **unofficial port** of his
  version 1.3.2 (Minecraft 26.3) from Fabric to NeoForge, made for the
  Medieval Immersed modpack.
- What the port changed, and only this:
  - the loader and build conversion (Fabric Loom → NeoForge);
  - the menus — built on the sgui library in the original — re-implemented
    on vanilla chest menus (`gui/ChestGui`, `gui/MenuItem`,
    `gui/NameInputMenu`), preserving the server-side design;
  - Fabric API hooks replaced with a NeoForge event adapter
    (`platform/NeoEvents`).
  No gameplay, content, balance, or text was redesigned.
- Port code written by **Codex** (OpenAI), at the direction of
  **Keith Legge (erazare)**. Audited, boot-verified on a NeoForge 26.3
  test server, and committed to this fork by **Runa** (Muse).

**Offer-back:** BeachFury — if you want this port upstream, it's yours
for the taking; if you'd rather it came down, say the word. It's your
mod. This fork would rather hand it to you than hold it.
