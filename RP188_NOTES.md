# RP188 — Daily rotation, channel viewport and photographic themes

Home uses its original background regardless of theme, on TV and mobile. The Home hero's gradients use the original dark canvas too. Theme colors and button styling remain active. Other pages use bundled photographic backgrounds; GSW retains its locally packaged team logo. Static WebP exports in app/src/main/res/drawable-nodpi are about 1280×720 and are drawn at controlled opacity for legibility. No runtime image downloads or animations are added. Player/Multiview retain black backgrounds.

Live's outer padding used to inset the header gradient and expose a bright band along the top/sides. Padding now belongs to the body, with the header extending across the screen. The channel grid loses the virtual Tirso tile and the M3U action, uses viewport-dependent card height, and reserves two title lines. Partially visible neighboring rows during scroll are normal; the focused card should fit the available viewport.

M3U URL/QR management moves to Live → Others → Settings. Scores, custom channels and sports-source settings remain available in the same menu. Kids/guest restrictions remain in place.

Tirso 24/7 becomes Home's Rotación diaria / Daily rotation row on both platforms. A date-seeded, mixed movie/episode selection is stable for the device's local day and rotates at midnight or app resume. Cards appear only after a source probe succeeds. Up to 14 candidates are probed, with up to eight cards displayed; positive and empty row results are cached in memory for five minutes by account/settings/day/filter. Source probing pauses when backgrounded or when opening playback. Clicking plays the selected exact episode/movie and returns to Home, with ordinary playback source resolution retained. No channel-hour UI remains. Adult/host restrictions remain; no schedule/video state is written to Supabase.

## Generated background assets

Production photographic-style backgrounds were generated for this project, then exported locally to WebP without changing their scenes. Common prompt: one wide cinematic photorealistic background, scenic detail on the right, calm dark negative space at left, restrained highlights, no text, logos, people, frames, interface or watermark. Scene prompts:

- christmas: Christmas: a warmly lit evergreen Christmas tree in an elegant dark timber cabin at night, small champagne-gold fairy lights and restrained red glass ornaments; snow and cool pine trees seen through a distant window, soft warm natural light, pine green, dark walnut and champagne gold, photorealistic cozy winter mood.
- halloween: Halloween: a tasteful autumn cottage porch at blue hour, a few beautifully carved real pumpkins glowing warm amber on stone steps on the right, wet fallen leaves, distant bare trees and very gentle natural mist. Deep plum-blue shadows, warm amber highlights; atmospheric sophisticated real photograph, no cartoonish art, no gore.
- summer: Summer: secluded tropical cove at late golden hour, translucent turquoise water and warm golden light grazing distant sand and a palm on the right, the left foreground softly shaded and calm. Rich deep teal shadows, luxurious peaceful coastal photograph, no resort furniture or people.
- fogata: Campfire: a small beautiful real campfire among dark forest rocks on the right at twilight, a few glowing copper embers and soft natural smoke, tactile wood and stone, warm firelight restrained against charcoal shadows. Cinematic outdoor photography, not a fireplace icon.
- sand: Sand: sweeping real desert dunes at dusk, delicate ripples in warm sand at lower right, muted amber light along dune crests, distant smoky taupe horizon, deep brown shadows, serene premium landscape photograph.
- storm: Storm: a real dramatic slate-blue storm front over a distant coastline at dusk, rain curtains and layered clouds, a very faint distant lightning vein at far right; cinematic and moody but not flashy, navy and silver-grey, dark calm foreground.
- nature: Nature: lush forested mountains and a pine grove on the right with morning mist between hills, soft diffused light, mossy greens and sage, foreground shaded, serene photorealistic woodland landscape.
- sea: Sea: calm deep-blue ocean at twilight with soft aquamarine surf moving around smooth dark rocks at far right, a quiet distant horizon and subtly luminous blue water, atmospheric realistic long-exposure coastal photograph, not stormy.

Validation targets: daily stability/change/variety, source gating and bounded probing, account/day cache isolation, expiry/manual retry, cancellation and saved Home row placement. APK release compilation and unit/lint checks cover both modules; physical-TV focus/overscan and final device appearance still require real-device testing.
