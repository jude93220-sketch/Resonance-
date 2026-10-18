package com.resonance

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.LruCache
import android.util.Size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val Accent = Color(0xFF00E5FF)
val PillGrey = Color(0xFF1C1C1C)

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
