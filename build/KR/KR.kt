import korlibs.audio.sound.*
import korlibs.io.file.*
import korlibs.io.file.std.*
import korlibs.image.bitmap.*
import korlibs.image.atlas.*
import korlibs.image.font.*
import korlibs.image.format.*

// AUTO-GENERATED FILE! DO NOT MODIFY!

@Retention(AnnotationRetention.BINARY) annotation class ResourceVfsPath(val path: String)
inline class TypedVfsFile(val __file: VfsFile)
inline class TypedVfsFileTTF(val __file: VfsFile) {
  suspend fun read(): korlibs.image.font.TtfFont = this.__file.readTtfFont()
}
inline class TypedVfsFileBitmap(val __file: VfsFile) {
  suspend fun read(): korlibs.image.bitmap.Bitmap = this.__file.readBitmap()
  suspend fun readSlice(atlas: MutableAtlasUnit? = null, name: String? = null): BmpSlice = this.__file.readBitmapSlice(name, atlas)
}
inline class TypedVfsFileSound(val __file: VfsFile) {
  suspend fun read(): korlibs.audio.sound.Sound = this.__file.readSound()
}
interface TypedAtlas<T>

object KR : __KR.KR

object __KR {
	
	interface KR {
		val __file get() = resourcesVfs[""]
		@ResourceVfsPath("korge.png") val `korge` get() = TypedVfsFileBitmap(resourcesVfs["korge.png"])
	}
}
