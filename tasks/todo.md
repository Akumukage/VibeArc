# VibeArc online-provider tasks

## Phase 1: release and license gate

- [ ] Decide whether VibeArc will be GPLv3-compatible and publish corresponding source.
- [ ] Confirm v0.7 is an experimental GitHub build, not a Google Play production release.

## Phase 2: provider foundation

- [ ] Define `MusicProvider`, provider track IDs, paged search results, and typed errors.
- [ ] Add fixture tests for valid, missing-field, unavailable, and temporary-failure responses.
- [ ] Implement public, unauthenticated InnerTube search and metadata.

### Checkpoint

- [ ] Provider tests pass and the Android app builds without NewPipeExtractor.

## Phase 3: playback and fallback

- [ ] Add NewPipeExtractor with the required API 26 core-library desugaring and pinned stable version.
- [ ] Resolve public playback URLs only when a user presses play.
- [ ] Fall back to NewPipeExtractor once for supported extraction failures.
- [ ] Connect remote tracks to Media3 without persisting or downloading media URLs.

### Checkpoint

- [ ] Search-to-play works for public test fixtures and fails clearly for unsupported content.
- [ ] Local-file playback still passes its existing tests.

## Phase 4: user experience and reliability

- [ ] Add YouTube attribution and provider-specific empty/error states.
- [ ] Add timeouts, cancellation, expiring-URL refresh, and minimal provider diagnostics.
- [ ] Test offline, slow-network, removed-content, region-blocked, and fallback flows.

## Release gate

- [ ] Publish privacy, copyright, attribution, and GPL notices.
- [ ] Keep offline downloads, DRM/ad bypasses, geographic bypasses, and credential capture out of scope.
- [ ] Do not call the provider stack production-ready or ship it to Google Play without written permission or a sanctioned replacement.
