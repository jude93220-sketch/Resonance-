package com.resonance

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val Accent = ResonanceUi.Accent
val PillGrey = ResonanceUi.SurfaceRaised

/** Loads and caches album art (embedded art or MediaStore thumbnail). */
object ArtLoader {
    private val cache = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }
    private val missing = HashSet<String>()

    suspend fun load(context: Context, song: Song, px: Int): Bitmap? {
        val key = "${if (song.albumId > 0) song.albumId else -song.id}_$px"
        cache.get(key)?.let { return it }
        if (synchronized(missing) { missing.contains(key) }) return null

        val bmp = withContext(Dispatchers.IO) { decode(context.applicationContext, song, px) }
        if (bmp != null) {
            cache.put(key, bmp)
        } else {
            synchronized(missing) { missing.add(key) }
        }
        return bmp
    }

    /** Average colour of the album art (or null if the song has no art). */
    suspend fun dominant(context: Context, song: Song): Int? {
        val bmp = load(context, song, 64) ?: return null
        return withContext(Dispatchers.Default) {
            val one = Bitmap.createScaledBitmap(bmp, 1, 1, true)
            one.getPixel(0, 0)
        }
    }

    private fun decode(context: Context, song: Song, px: Int): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.contentResolver.loadThumbnail(song.uri, Size(px, px), null)
            } else {
                val r = MediaMetadataRetriever()
                try {
                    r.setDataSource(context, song.uri)
                    val bytes = r.embeddedPicture
                    if (bytes == null) null else decodeSampled(bytes, px)
                } finally {
                    r.release()
                }
            }
        } catch (t: Throwable) {
            null
        }
    }

    private fun decodeSampled(bytes: ByteArray, px: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= px && bounds.outHeight / (sample * 2) >= px) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }
}

/** Reads the real sample rate / bitrate of a file, e.g. "48 KHZ  160 KBPS  MP3". */
object AudioInfo {
    private val cache = HashMap<Long, String>()

    suspend fun label(context: Context, song: Song): String {
        val cached = synchronized(cache) { cache[song.id] }
        if (cached != null) return cached
        val text = withContext(Dispatchers.IO) { compute(context.applicationContext, song) }
        synchronized(cache) { cache[song.id] = text }
        return text
    }

    private fun compute(context: Context, song: Song): String {
        var sampleRate = 0
        var bitRate = 0
        try {
            val ex = MediaExtractor()
            try {
                ex.setDataSource(context, song.uri, null)
                if (ex.trackCount > 0) {
                    val f = ex.getTrackFormat(0)
                    if (f.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        sampleRate = f.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    }
                    if (f.containsKey(MediaFormat.KEY_BIT_RATE)) {
                        bitRate = f.getInteger(MediaFormat.KEY_BIT_RATE)
                    }
                }
            } finally {
                ex.release()
            }
        } catch (t: Throwable) {
        }
        if (bitRate <= 0) {
            try {
                val r = MediaMetadataRetriever()
                try {
                    r.setDataSource(context, song.uri)
                    bitRate = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                        ?.toIntOrNull() ?: 0
                } finally {
                    r.release()
                }
            } catch (t: Throwable) {
            }
        }
        val parts = ArrayList<String>()
        if (sampleRate > 0) {
            parts.add(
                if (sampleRate % 1000 == 0) "${sampleRate / 1000} KHZ"
                else "%.1f KHZ".format(sampleRate / 1000f)
            )
        }
        if (bitRate > 0) parts.add("${bitRate / 1000} KBPS")
        parts.add(song.ext.uppercase())
        return parts.joinToString("  ")
    }
}

/** A colour taken from the current album art; the whole app background fades to it. */
@Composable
fun rememberArtTint(song: Song?): Color {
    val context = LocalContext.current
    var argb by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(song?.id) {
        if (song != null) {
            argb = ArtLoader.dominant(context, song)
        }
    }

    val base = argb
    val target = if (base == null) {
        Color(0xFF0F2A44)
    } else {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(base, hsv)
        hsv[1] = (hsv[1] * 1.15f).coerceIn(0f, 1f)
        hsv[2] = hsv[2].coerceIn(0.30f, 0.65f)
        Color(android.graphics.Color.HSVToColor(hsv))
    }
    val animated by animateColorAsState(target, tween(700), label = "artTint")
    return animated
}

@Composable
fun AlbumArt(song: Song?, px: Int, modifier: Modifier = Modifier, iconSize: Dp = 24.dp) {
    val context = LocalContext.current
    var bitmap by remember(song?.id) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(song?.id, px) {
        bitmap = if (song == null) null else ArtLoader.load(context, song, px)
    }

    Box(modifier.background(Color(0xFF152238)), contentAlignment = Alignment.Center) {
        val b = bitmap
        if (b != null) {
            Image(
                bitmap = b.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                Icons.Filled.MusicNote,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.25f),
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

/** Wide album-art card: the art sits in the middle over a blurred copy of itself. */
@Composable
fun ArtCard(song: Song?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var bitmap by remember(song?.id) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(song?.id) {
        bitmap = if (song == null) null else ArtLoader.load(context, song, 1024)
    }

    Box(modifier.background(Color(0x33000000)), contentAlignment = Alignment.Center) {
        val b = bitmap
        if (b != null) {
            val img = b.asImageBitmap()
            Image(
                bitmap = img,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(32.dp),
            )
            Image(
                bitmap = img,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                Icons.Filled.MusicNote,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.25f),
                modifier = Modifier.size(96.dp),
            )
        }
    }
}
