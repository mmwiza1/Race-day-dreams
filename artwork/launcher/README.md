# Approved launcher artwork

`approved.jpg` is the official illustration supplied and approved by the repository owner.
No third-party icon library, generated replacement illustration, text or branding is added.

Run `scripts/generate-launcher-icons.sh` with ImageMagick 7 to regenerate the PNGs.
The script trims only the outer white presentation margin/shadow and removes connected
outer background pixels. It preserves the illustration's proportions and interior colours.
The original JPEG is retained here, outside packaged application resources.

The full-colour foreground occupies 66dp of a transparent 108dp adaptive canvas, over a
solid blue background. Android normally displays the central 72dp. This leaves room for
the sleeping face, curls, dream bubble, runner and ribbon under common launcher masks.
Legacy icons are 48/72/96/144/192px with matching visible scale; foreground layers are
108/162/216/324/432px. Both regular and round manifest icons use adaptive XML on API 26+.
API 33+ adds an original, simplified white-alpha sleeping/dream-runner vector so the
launcher can choose themed-icon colours. It is intentionally distinct from the detailed
colour illustration. Notifications retain their existing small symbol as `ic_notification`.

`docs/launcher/mask-preview.png` is a deterministic composition preview of the actual
foreground/background resources, using circle, rounded-square and squircle masks.
It is an icon mask preview, not a screenshot of a physical launcher. Check Samsung One UI
at normal size and with themed icons/light/dark mode; OEM masks and motion effects vary.
