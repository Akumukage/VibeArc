# Implementation Plan: VibeArc v0.7 to v1.0

## Overview

Replace the planned Audius integration with an experimental YouTube music
provider. VibeArc will use a keyless InnerTube client for primary search and
metadata, a YT client profile for public playback resolution, and
NewPipeExtractor as the secondary resolver. Local files remain fully supported.

This is technically feasible for a sideloaded/open-source experiment, but it is
not a stable or policy-compliant route to a Google Play production release.

## Architecture decisions

- Keep online sources behind one small `MusicProvider` contract so local
  playback and the UI do not depend on InnerTube or NewPipe response shapes.
- Use InnerTube first; retry once through NewPipeExtractor only for supported
  public content and known extraction failures.
- Validate all provider responses and expose typed unavailable, region-blocked,
  authentication-required, and temporary-failure results.
- Resolve stream URLs only when playback starts. Do not persist expiring URLs.
- Cache metadata briefly, but never download or cache YouTube media.
- Do not accept Google credentials, cookies, passkeys, or CAPTCHA solutions.
- Do not bypass DRM, advertisements, geographic restrictions, age gates,
  membership checks, or bot challenges.

## Feasibility

| Capability | Status | Boundary |
|---|---|---|
| Search public YouTube/YouTube Music metadata | Implementable, experimental | Unofficial InnerTube responses can change without notice. |
| Show titles, artists, artwork, albums, and durations | Implementable | Validate missing or changed fields and attribute YouTube as the source. |
| Resolve and play public streams with Media3 | Technically implementable | URLs expire; extraction may fail or be blocked. Not promised as a stable service. |
| Fall back from InnerTube to NewPipeExtractor | Implementable | Use one bounded fallback, not retry loops or restriction bypasses. |
| Queues, favorites, playlists, and recent history | Implementable | Store provider IDs and metadata, not media files or expiring stream URLs. |
| Account-free playback of every track | Not implementable | Private, premium, age-gated, members-only, DRM, region-blocked, and bot-protected content cannot be guaranteed. |
| Permanent reliable access | Not implementable | InnerTube is undocumented and extractor updates routinely follow YouTube changes. |
| Offline YouTube downloads | Excluded | Conflicts with YouTube's published restrictions and the project safety boundary. |
| Ad removal, DRM bypass, or geographic bypass | Excluded | Will not be implemented. |
| Play Store-safe background YouTube audio | Not implementable with this design | YouTube policies prohibit isolated audio and background playback for API clients. |
| Closed-source distribution with NewPipeExtractor | Not compatible | NewPipeExtractor is GPLv3-or-later; distribution requires a GPL-compatible open-source release and corresponding source. |
| Production `v1.0` using this stack without approval | Blocked | Requires written YouTube permission/licensing or replacement with a sanctioned music catalog/provider. |

## Version roadmap

### v0.7 — Experimental provider foundation

- Define the provider contract and typed errors.
- Add InnerTube search and metadata for public, unauthenticated content.
- Resolve playback on demand through the YT client, with NewPipeExtractor as a
  bounded fallback.
- Add clear YouTube attribution, provider error states, and a local-files-only
  fallback when online playback fails.
- Ship only as an experimental GitHub build until licensing and policy review is
  complete.

### v0.8 — Reliability and provider health

- Add contract fixtures for response changes and extractor failures.
- Add expiring-URL refresh, cancellation, timeouts, and provider health logging.
- Preserve queue state without persisting remote media URLs.
- Test slow networks, no network, removed content, and fallback behavior.

### v0.9 — Community beta and release decision

- Publish privacy, copyright, provider-attribution, and GPL notices.
- Run device testing and community beta feedback.
- Decide between an experimental open-source distribution and a sanctioned
  provider suitable for stores.

### v1.0 — Conditional production release

- Release as production only after the catalog/playback source has written
  permission or a compliant licensed provider replaces the experimental stack.
- If that gate is not met, keep the InnerTube/NewPipe build pre-1.0 and label it
  experimental.

## Dependency and license gate

NewPipeExtractor supports independent Android use, but its documentation says
that projects below Android API 33 require core-library desugaring. VibeArc has
`minSdk 26`, so v0.7 must add that configuration. Its GPLv3-or-later license also
means VibeArc must adopt a compatible license and publish corresponding source
before distributing a build that contains the library. No extractor dependency
will be added until that licensing decision is explicit.

## Primary risks

| Risk | Impact | Mitigation |
|---|---|---|
| YouTube changes internal responses or signatures | High | Provider boundary, fixtures, one fallback, rapid dependency updates. |
| Short-lived or throttled stream URLs | High | Resolve at play time; refresh once; surface failure clearly. |
| GPL obligations are missed | High | License gate before adding NewPipeExtractor. |
| Store rejection or YouTube enforcement | High | Do not call this production-ready; use a sanctioned provider for store release. |
| Unexpected provider data | Medium | Validate at the provider boundary and reject malformed results. |

## Source basis

- YouTube Terms restrict automated access, downloading, and unapproved content
  use: https://www.youtube.com/static?template=terms
- YouTube Developer Policies prohibit scraping, isolated audio, background
  playback, offline copies, ad interference, and restriction bypasses:
  https://developers.google.com/youtube/terms/developer-policies
- YouTube's policy guide repeats the download, audio separation, and background
  playback restrictions:
  https://developers.google.com/youtube/terms/developer-policies-guide
- NewPipeExtractor usage, Android desugaring requirement, supported services,
  and GPL terms:
  https://github.com/TeamNewPipe/NewPipeExtractor/blob/dev/README.md

Reviewed on 2026-09-24. This plan is an engineering assessment, not legal advice.
