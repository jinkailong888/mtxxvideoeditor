package tv.danmaku.ijk.media.player;

import android.graphics.SurfaceTexture;
import android.view.Surface;

/**
 * Created by jieping on 2018/3/17.
 */

public class MySurface extends Surface {
    public int width;
    public int height;
    public MySurface(SurfaceTexture surfaceTexture, int width, int height) {
        super(surfaceTexture);
        this.width = width;
        this.height = height;
    }
}
