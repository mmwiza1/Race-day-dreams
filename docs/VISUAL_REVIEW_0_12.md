# Material UI visual review — 0.12.0-test1

This review uses unedited PNGs from actual production screens with instrumentation-only
synthetic data. Design references remain separate. PNG integrity and semantics assertions
support the review; neither replaces inspection of the rendered page.

## Observed checkpoint

Source `bed9a81a611cc9793ab4bf96314604b668e97c5b`, complete workflow42265222799:
517 full cases and383 authentic captures on each API28/35/36. Canonical862117f preserves all
API35 originals. Every383 PNG byte matches downloaded artifact13528616131; ZIP SHA256:
`73bebd47fd56a53344c12c474a009fdd09efec0d3c98e912be1f01edc8fe30d7`.
Fingerprint `a88c2576b4908314f161f9a46c2df81ffa949f05a7c7ba934dd8e567407ef92c`
matches actual source. API36 artifact13528298057 contains383 valid captures, ZIP SHA256
`d4513989f49c117ed02409bf3dd44821c4d0b5243f59c124a0a7ee75a3b8b6d9`.
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
