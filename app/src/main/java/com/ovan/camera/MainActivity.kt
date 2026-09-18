package com.ovan.camera

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.view.MotionEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ovan.camera.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
 private lateinit var b: ActivityMainBinding
 private var capture: ImageCapture? = null
 private var camera: Camera? = null
 private var lens = CameraSelector.LENS_FACING_BACK
 private var flash = ImageCapture.FLASH_MODE_AUTO
 private var mode = 0
 private val modes = listOf("SMART HDR","NATURAL","IPHONE STYLE","NIGHT","PORTRAIT")
 override fun onCreate(s: Bundle?) { super.onCreate(s); b=ActivityMainBinding.inflate(layoutInflater); setContentView(b.root)
  if (hasPermission()) startCamera() else ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.CAMERA),10)
  b.shutterButton.setOnClickListener{takePhoto()}
  b.switchButton.setOnClickListener{lens=if(lens==CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK; startCamera()}
  b.flashButton.setOnClickListener{flash=when(flash){ImageCapture.FLASH_MODE_AUTO->ImageCapture.FLASH_MODE_ON;ImageCapture.FLASH_MODE_ON->ImageCapture.FLASH_MODE_OFF;else->ImageCapture.FLASH_MODE_AUTO}; b.flashButton.text=when(flash){ImageCapture.FLASH_MODE_AUTO->"⚡ A";ImageCapture.FLASH_MODE_ON->"⚡ ON";else->"⚡ OFF"}; capture?.flashMode=flash}
  b.modeButton.setOnClickListener{mode=(mode+1)%modes.size; updateMode()}
  b.exposureSeek.setOnSeekBarChangeListener(object:android.widget.SeekBar.OnSeekBarChangeListener{override fun onProgressChanged(x:android.widget.SeekBar?,p:Int,f:Boolean){camera?.cameraControl?.setExposureCompensationIndex(p-100)};override fun onStartTrackingTouch(x:android.widget.SeekBar?){};override fun onStopTrackingTouch(x:android.widget.SeekBar?){}})
 }
 private fun updateMode(){b.modeButton.text=modes[mode];b.modeText.text="OVAN ${modes[mode]}";b.statusText.text=when(mode){0->"Multi-frame ready • HDR • Smart Tone";1->"Natural color • Skin tone • Detail";2->"Soft highlights • Natural contrast";3->"Low-light capture • Noise control";else->"Subject focus • Natural background blur"}; b.exposureSeek.visibility=if(mode==3) android.view.View.VISIBLE else android.view.View.GONE}
 private fun startCamera(){val f=ProcessCameraProvider.getInstance(this);f.addListener({val p=f.get();val sel=CameraSelector.Builder().requireLensFacing(lens).build();val preview=Preview.Builder().build().also{it.surfaceProvider=b.previewView.surfaceProvider};capture=ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).setFlashMode(flash).build();p.unbindAll();camera=p.bindToLifecycle(this,sel,preview,capture);b.previewView.setOnTouchListener{_,e->if(e.action==MotionEvent.ACTION_UP){val pt=b.previewView.meteringPointFactory.createPoint(e.x,e.y);camera?.cameraControl?.startFocusAndMetering(FocusMeteringAction.Builder(pt).build())};true}},ContextCompat.getMainExecutor(this))}
 private fun takePhoto(){val c=capture?:return;val v=ContentValues().apply{put(MediaStore.Images.Media.DISPLAY_NAME,"OVAN_${System.currentTimeMillis()}.jpg");put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg");put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/OVAN Camera")};val out=ImageCapture.OutputFileOptions.Builder(contentResolver,MediaStore.Images.Media.EXTERNAL_CONTENT_URI,v).build();c.takePicture(out,ContextCompat.getMainExecutor(this),object:ImageCapture.OnImageSavedCallback{override fun onImageSaved(r:ImageCapture.OutputFileResults){Toast.makeText(this@MainActivity,"Foto tersimpan • OVAN Camera V3",Toast.LENGTH_SHORT).show()};override fun onError(e:ImageCaptureException){Toast.makeText(this@MainActivity,"Gagal: ${e.message}",Toast.LENGTH_SHORT).show()}})}
 private fun hasPermission()=ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED
 override fun onRequestPermissionsResult(r:Int,p:Array<out String>,g:IntArray){super.onRequestPermissionsResult(r,p,g);if(r==10&&hasPermission())startCamera()}
}
