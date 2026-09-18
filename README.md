# OVAN Camera V4

OVAN Camera V4 adalah aplikasi kamera Android native berbasis Kotlin + CameraX.

## Fitur V4
- CameraX photo capture
- Kamera belakang/depan
- Tap-to-focus dengan focus marker
- Zoom 1×–8×
- Flash Auto / On / Off
- Timer 3 detik / 10 detik
- Grid rule-of-thirds
- Preset Smart HDR, Natural, iPhone Style, Night, Portrait
- Kontrol exposure pada preset Night
- Tombol galeri
- APK debug otomatis dibangun oleh Railway

> Catatan: nama preset seperti Smart HDR adalah preset UI/kontrol. V4 belum mengklaim melakukan computational HDR multi-frame.

## Railway
Repository harus memiliki `Dockerfile` di root. Railway akan membangun Docker image lalu menjalankan server Python untuk menyediakan APK.

File hasil build: `OVAN-Camera-V4-debug.apk`.
