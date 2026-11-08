# Material UI visual review — 0.12.0-test1

This review uses unedited PNGs from actual production screens with instrumentation-only
synthetic data. Design references remain separate. PNG integrity and semantics assertions
support the review; neither replaces inspection of the rendered page.

## Observed checkpoint

Source `5d2814331af3f189f38f97d72974b242e398afe8`, complete workflow42518618237:
517 full cases and383 authentic captures on each API28/35/36. Canonical `e0bb250` preserves all
API35 originals. Every383 PNG byte matches downloaded artifact13632724252; ZIP SHA256:
`f9f832bcd00dcd3608cbc26cd1b1d2bf366ad569e5693fa066f08041e2c119c7`.
Fingerprint `c1ad95aadd1098d4db00b44e739e8f3150936d859cebc55fc29abe5a1e7d6faf`
matches actual source. API36 artifact13632414462 contains383 valid captures, ZIP SHA256
`71198d99512b989d25f239b599b92e551c77f165ecb04036a2d0b4bc60bf59a5`.
No image editing, replacement mockup or test-only substitute screen is used.

Direct inspection covered:

- All six branded/device/light/dark/200% Home and six live-running variants, both
  large-text Finish confirmations, and short-wide Home/Plan navigation.
- All four public native permission views: light, dark, large-text top and scrolled actions.
- Progression entry, dark expanded details, large-text unchanged facts, collapsed controls
  and expanded-evidence actions; Gym Now at normal/large text; dark analytics and large
  breakdowns; recovery, protein and storage summary/disclosure/confirmation views.
- Calendar history, activity summary, running trends, shoes, Race Day and health context.

The hierarchy is coherent: today's workout and action precede the goal/upcoming sessions;
live distance/time/pace are legible; technical details remain optional. Measured, estimated,
unknown and synthetic fixture values retain their existing labels. The strength suggestion
still distinguishes its optional82kg reference from actual logged80kg evidence and keeps
Save separate from activation. Large-text actions wrap or stack without horizontal scrolling.

No blocking app-owned clipping, overlap or unreadable contrast was found in this sample.
Ordinary viewport-edge cropping is scrollable content, not lost text. Native Close is fully
visible above the navigation inset at200%; the scrolled privacy text clips below the31px
status strip. Light/dark phone and native system icons have contrasting backgrounds.

## Final review and retained limitations

Actual API36 branded light Home/live,200% Home/live, device-dark live, native permission footer
and short-wide views were opened directly. Light live and large-text phone navigation buttons
are now clearly dark against the pale surface; dark mode retains bright icons. This closes the
previous phone-bar visual blocker. Appearance flags alone were not accepted as evidence.
Application content meets the intended Material You hierarchy/shape/colour direction while
preserving the established illustrated guidance and Dream Route identity. No claim of full
experimental Expressive API adoption or physical-device acceptance is made.

Forced1400x720 emulator captures show a reserved bottom platform navigation strip without
three-button icons. The app's rail remains reachable and its content scrolls; normal phone
captures show the icons. This synthetic resize observation is retained, not cropped out or
claimed as hardware verification. Real rotation/system-navigation checks remain on the owner
checklist.

Human TalkBack, Galaxy S24/Pixel/Xiaomi, real display densities, keyboards, providers, audio,
locked-screen/battery behaviour and outdoor GPS remain unperformed hardware acceptance.
