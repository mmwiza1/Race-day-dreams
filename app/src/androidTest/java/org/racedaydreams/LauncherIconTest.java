package org.racedaydreams;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.test.AndroidTestCase;

/** Exercises the packaged manifest/resource selection on every supported CI API. */
@SuppressWarnings("deprecation")
public final class LauncherIconTest extends AndroidTestCase {
    public void testPackagedLauncherUsesApprovedAdaptiveArtwork() {
        assertEquals(R.mipmap.ic_launcher, getContext().getApplicationInfo().icon);
        for (int resource : new int[]{R.mipmap.ic_launcher, R.mipmap.ic_launcher_round}) {
            Drawable drawable = getContext().getDrawable(resource);
            assertTrue(drawable instanceof AdaptiveIconDrawable);
            AdaptiveIconDrawable icon = (AdaptiveIconDrawable) drawable;
            assertNotNull(icon.getBackground());
            assertTrue(icon.getForeground() instanceof BitmapDrawable);
            Bitmap artwork = ((BitmapDrawable) icon.getForeground()).getBitmap();
            // Padded illustration is coloured and transparent outside its safe-area bounds.
            assertEquals(0, artwork.getPixel(0, 0) >>> 24);
            assertTrue((artwork.getPixel(artwork.getWidth()/2, artwork.getHeight()/2) >>> 24) > 0);
            if (Build.VERSION.SDK_INT >= 33) assertNotNull(icon.getMonochrome());
            for (int size : new int[]{48, 96, 192}) {
                Bitmap result = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
                icon.setBounds(0, 0, size, size);
                icon.draw(new Canvas(result));
                assertTrue((result.getPixel(size/2, size/2) >>> 24) > 0);
                result.recycle();
            }
        }
        assertNotNull(getContext().getDrawable(R.drawable.ic_notification));
    }
}
