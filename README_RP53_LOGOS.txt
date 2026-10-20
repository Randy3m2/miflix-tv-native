BruniO RP53 - Local logo fallback hotfix

This hotfix changes only the Tirso TV channel catalog and adds 121 locally
generated, channel-specific PNG fallback cards (not copyrighted official artwork).

- Existing network/TV logos are kept unchanged.
- Previously generic movie/24-7 logos and blank logos receive distinct
  local cards labeled with their correct channel name.
- No stream URLs, categories, channel IDs, or app version are changed.
- Apply after previous RP53 hotfixes.
- Assets are referenced as file:///android_asset/tirso_logos/NNN.png.
- Coil3 on Android TV must support Android asset file URIs; verify on device.
- Real online poster/logo downloads could not be verified in this execution
  environment (external image hosts are unreachable).
- Remote logos retained in the catalog have not been individually verified.

This is a functional fallback patch, NOT the full online-artwork audit.
