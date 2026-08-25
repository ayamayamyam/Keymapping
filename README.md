# Gamepad Mapper

Aplikasi Android sederhana yang menampilkan tombol virtual (overlay) di atas
aplikasi/game lain. Saat tombol ditekan, aplikasi mensimulasikan tap di
koordinat yang sama menggunakan Accessibility Service, sehingga game di
bawahnya menerima sentuhan tersebut.

## Cara Membuka Project
1. Clone repo ini.
2. Buka folder project di **Android Studio** (File > Open).
3. Tunggu proses Gradle sync selesai.
4. Jalankan (Run) ke emulator atau HP Android asli (disarankan HP asli,
   karena fitur overlay & accessibility service butuh interaksi izin manual).

## Cara Menggunakan Aplikasi
1. Buka aplikasi, tekan **"1. Izinkan Overlay"** — aktifkan izin
   "Display over other apps" untuk aplikasi ini.
2. Tekan **"2. Aktifkan Accessibility Service"** — cari "Gamepad Mapper" di
   daftar, lalu aktifkan.
3. Buka game/aplikasi target, lalu kembali ke Gamepad Mapper dan tekan
   **"3. Tampilkan Tombol Virtual"**.
4. Tombol-tombol akan muncul melayang di atas layar:
   - **Tap singkat** pada tombol → mensimulasikan tap ke game di posisi itu.
   - **Tahan & geser** tombol → memindahkan posisinya (tersimpan otomatis).
5. Tekan **"Sembunyikan Tombol Virtual"** untuk menyembunyikan overlay.

## Struktur Kode
- `MainActivity.kt` — UI utama, cek & minta izin.
- `TapAccessibilityService.kt` — mensimulasikan tap lewat `dispatchGesture`.
- `OverlayService.kt` — menggambar tombol-tombol virtual & menangani
  drag/tap.
- `ButtonMapping.kt` — model data tombol + penyimpanan ke `SharedPreferences`.

## Catatan Penting
- Beberapa game modern (khususnya game online kompetitif) mendeteksi dan
  memblokir Accessibility Service/overlay untuk mencegah kecurangan.
  Gunakan aplikasi ini sesuai ketentuan layanan (ToS) game yang dimainkan.
- Ini adalah kerangka dasar (starter project) — belum ada UI untuk
  menambah/menghapus tombol secara dinamis dari dalam aplikasi. Posisi
  tombol default ada di `MappingStore.defaultMappings()`.
