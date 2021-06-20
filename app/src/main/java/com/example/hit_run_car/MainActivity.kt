package com.example.hit_run_car

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_report.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity() {
    val urls = "http://34.64.237.147:5000/predict"
    val OPEN_GALLETY = 2
    val REQUEST_IMAGE_CAPTURE = 1
    var CheckImageSelected = 0
    lateinit var reportResult : String
    lateinit var currentPhotoPath : String
    val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    val PERMISSIONS_REQUEST_CODE = 100



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        settingPermission() // 권한체크 시작
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE)
        btn_analysis.isEnabled = false
        val reportIntent = Intent(this, reportActivity::class.java)

        btn_take_picture.setOnClickListener {
            startCapture()
        }
        btn_bring_picture.setOnClickListener {
            openGallery()
        }
        btn_analysis.setOnClickListener{
            connectServer()
            startActivity(reportIntent)
        }

    }

    private fun connectServer() {
        if (CheckImageSelected == 0) {
            Toast.makeText(this@MainActivity, "선택된 사진이 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(this@MainActivity, "사진을 서버로 보내는 중입니다.", Toast.LENGTH_SHORT).show()
        var imgfile : File = File(currentPhotoPath)
        val postBodyImage : RequestBody = MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart(
            "file", "androidToFlask.jpg",
            imgfile.asRequestBody("image/jpg".toMediaTypeOrNull())
        ).build()
        thread(start = true) {
            postRequest(postBodyImage)
        }
    }

    @SuppressLint("SetTextI18n")
    fun postRequest(postBody: RequestBody) {
        val client = OkHttpClient()
        val request  = Request.Builder()
            .url(urls)
            .post(postBody)
            .build();
        val call : Call = client.newCall(request)
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                call.cancel()
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "서버접속에 실패하였습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    reportResult = response?.body?.string().toString().split('"')[7]
                    Toast.makeText(this@MainActivity, reportResult, Toast.LENGTH_SHORT).show()
                    try {
                        println(reportResult)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        })
    }

    private fun openGallery () {
        val intent: Intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.setType("image/*")
        startActivityForResult(intent, OPEN_GALLETY)
    }

    private fun settingPermission(){
        var permis = object  : PermissionListener{
            //            어떠한 형식을 상속받는 익명 클래스의 객체를 생성하기 위해 다음과 같이 작성
            override fun onPermissionGranted() {
                Toast.makeText(this@MainActivity, "권한 허가", Toast.LENGTH_SHORT)
                    .show()
            }

            override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
                Toast.makeText(this@MainActivity, "권한 거부", Toast.LENGTH_SHORT)
                    .show()
                ActivityCompat.finishAffinity(this@MainActivity) // 권한 거부시 앱 종료
            }
        }

        TedPermission.with(this)
            .setPermissionListener(permis)
            .setRationaleMessage("카메라 사진 권한 필요")
            .setDeniedMessage("카메라 권한 요청 거부")
            .setPermissions(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA)
            .check()
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun startCapture(){
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                val photoFile: File? = try{
                    createImageFile()
                }catch(ex:IOException){
                    null
                }
                photoFile?.also{
                    val photoURI : Uri = FileProvider.getUriForFile(
                        this,
                        "com.example.hit_run_car.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    @Throws(IOException::class)
    private fun createImageFile() : File{
        val timeStamp : String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir : File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply{
            currentPhotoPath = absolutePath
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        var latitude : Float = 0F
        var longitude : Float = 0F

        if(resultCode == Activity.RESULT_OK){
            lateinit var exif : ExifInterface
            var exifOrientation : Int = 0
            var exifDegree : Int = 0
            var exifDatetime : Any? = null
            var exifLatitude : String = ""
            var exifLatitude_ref : String = ""
            var exifLongitude : String = ""
            var exifLongitude_ref : String = ""
            var mResultList: List<Address>? = null

            if(requestCode == REQUEST_IMAGE_CAPTURE) {
                val bitmap = BitmapFactory.decodeFile(currentPhotoPath)

                try{
                    exif = ExifInterface(currentPhotoPath)
                    exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL)
                    exifDegree = exifOrientationToDegree(exifOrientation)
                    imageView.setImageBitmap(rotate(bitmap, exifDegree))
                    btn_analysis.isEnabled = true
                    CheckImageSelected = 1
                }catch (e : IOException){
                    e.printStackTrace()
                }
            }
            else if (requestCode == OPEN_GALLETY) {
                var currentImageUrl : Uri? = data?.data
                try {
                    CheckImageSelected = 1
                    btn_analysis.isEnabled = true
                    currentPhotoPath = currentImageUrl?.let { getPathFromURI(this, it) }.toString()
                    var inputstream : InputStream? = contentResolver.openInputStream(currentImageUrl!!)
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, currentImageUrl)
                    exif = ExifInterface(inputstream!!)
                    exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL)
                    exifDegree = exifOrientationToDegree(exifOrientation)
                    imageView.setImageBitmap(rotate(bitmap, exifDegree))

                }catch (e:Exception) {
                    e.printStackTrace()
                }
            }
            println(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE).toString())
            println("here")
            if (exif != null) {
                exifDatetime  = exif.getAttribute(ExifInterface.TAG_DATETIME)

                if(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE) != null &&
                    exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE) != null ){

                    exifLatitude = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE).toString()
                    exifLatitude_ref = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF).toString()
                    exifLongitude = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE).toString()
                    exifLongitude_ref = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF).toString()
                    Log.d("pictures metadata : ", exifLatitude+"\n")
                    Log.d("pictures metadata : ", exifLatitude_ref+"\n")
                    Log.d("pictures metadata : ", exifLongitude+"\n")
                    Log.d("pictures metadata : ", exifLongitude_ref+"\n")


                    if (exifLatitude_ref.equals("N")) {
                        latitude = convertToDegree(exifLatitude);
                    } else {
                        latitude = 0 - convertToDegree(exifLatitude);
                    }

                    if (exifLongitude_ref.equals("E")) {
                        longitude = convertToDegree(exifLongitude);
                    } else {
                        longitude = 0 - convertToDegree(exifLongitude);
                    }
                    Log.d("picture's degree : ", exifDegree.toString())
                    Log.d("picture's datetime : ", exifDatetime.toString())
                    Log.d("picture's latitude : ", latitude.toString())
                    Log.d("picture's longitude : ", longitude.toString())
                    var mGeoCoder =  Geocoder(applicationContext, Locale.KOREAN)

                    try{
                        mResultList = mGeoCoder.getFromLocation(
                            latitude.toDouble()!!, longitude.toDouble()!!, 1
                        )
                    }catch(e: IOException){
                        e.printStackTrace()
                    }
                    if(mResultList != null){
                        Log.d("picture's 주소", mResultList[0].getAddressLine(0))
                    }
                }
            }
            Log.d("picture's datetime : ", exifDatetime.toString())

            photo_date_value.text = (exifDatetime).toString()
            photo_location_value.text = mResultList?.get(0)?.getAddressLine(0)
        }

    }

    private fun exifOrientationToDegree(exifOrientation: Int): Int {
        Log.d("orientation", exifOrientation.toString())
        when(exifOrientation){
            ExifInterface.ORIENTATION_ROTATE_90 ->{
                Log.d("rotate","rotate90")
                return 90
            }
            ExifInterface.ORIENTATION_ROTATE_180 -> {
                Log.d("rotate","rotate180")
                return 180
            }
            ExifInterface.ORIENTATION_ROTATE_270 ->{
                Log.d("rotate","rotate270")
                return 270
            }
            else -> {
                Log.d("rotate","rotate0")
                return 0
            }
        }
    }

    private fun rotate(bitmap: Bitmap, degree: Int) : Bitmap {
        Log.d("rotate","init rotate")
        val matrix = Matrix()
        matrix.postRotate(degree.toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix,true)
    }

    //사진 메타데이터를 위도 경도 값으로 예쁘게 정리해서 뽑아주는 함수

    private fun convertToDegree(stringDMS : String): Float {
        Log.d("stringDMs : ",stringDMS)
        var result : Float = 0F
        var DMS = stringDMS.split(",")
        var stringD = DMS[0].split("/")
        Log.d("stringD : ",stringD.toString())
        var D0 = stringD[0].toDouble()
        var D1 = stringD[1].toDouble()
        var FloatD = D0 / D1

        var stringM = DMS[1].split("/");
        var M0 = stringM[0].toDouble()
        var M1 = stringM[1].toDouble()
        var FloatM = M0 / M1

        var stringS = DMS[2].split("/");
        var S0 = stringS[0].toDouble()
        var S1 = stringS[1].toDouble()
        var FloatS = S0 / S1

        result = (FloatD + (FloatM / 60) + (FloatS / 3600)).toFloat()

        return result
    }

    //현재 위도경도를 출력하는 함수와 코드들
    private fun getLocation(){
        var locatioNManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager?
        var userLocation: Location = getLatLng(locatioNManager as LocationManager)
        if(userLocation != null){
            var latitude = userLocation.latitude
            var longitude = userLocation.longitude
            Log.d("CheckCurrentLocation", "현재 내 위치 값: ${latitude}, ${longitude}")

            var mGeoCoder =  Geocoder(applicationContext, Locale.KOREAN)
            var mResultList: List<Address>? = null
            try{
                mResultList = mGeoCoder.getFromLocation(
                    latitude!!, longitude!!, 1
                )
            }catch(e: IOException){
                e.printStackTrace()
            }
            if(mResultList != null){
                Log.d("CheckCurrentLocation", mResultList[0].getAddressLine(0))
            }
        }
    }

    private fun getLatLng(locatioNManager : LocationManager): Location{
        var currentLatLng: Location? = null
        var hasFineLocationPermission = ContextCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_FINE_LOCATION)
        var hasCoarseLocationPermission = ContextCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_COARSE_LOCATION)

        if(hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
            hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED){
            val locatioNProvider = LocationManager.GPS_PROVIDER
            currentLatLng = locatioNManager?.getLastKnownLocation(locatioNProvider)
        }else{
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])){
                Toast.makeText(this, "앱을 실행하려면 위치 접근 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE)
            }else{
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE)
            }
            currentLatLng = getLatLng(locatioNManager)
        }
        return currentLatLng!!
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == PERMISSIONS_REQUEST_CODE && grantResults.size == REQUIRED_PERMISSIONS.size) {
            var check_result = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    check_result = false;
                    break;
                }
            }
            if (check_result) {
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        REQUIRED_PERMISSIONS[0]
                    )
                    || ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        REQUIRED_PERMISSIONS[1]
                    )
                ) {
                    Toast.makeText(
                        this,
                        "권한 설정이 거부되었습니다.\n앱을 사용하시려면 다시 실행해주세요.",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                } else {
                    Toast.makeText(this, "권한 설정이 거부되었습니다.\n설정에서 권한을 허용해야 합니다..", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }
    //Uri로 부터 절대경로 알아내는 함수
    private  fun  getPathFromURI(context: Context, uri: Uri) : String? {
        var fullPath: String? = null
        val column = "_data"
        var cursor: Cursor? = context.getContentResolver().query(uri, null, null, null, null)
        if (cursor != null) {
            cursor.moveToFirst()
            var document_id: String = cursor.getString(0)
            document_id = document_id.substring(document_id.lastIndexOf(":") + 1)
            cursor.close()
            val projection = arrayOf(column)
            try {
                cursor = context.contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    MediaStore.Images.Media._ID + " = ? ",
                    arrayOf(document_id),
                    null
                )
                if (cursor != null) {
                    cursor.moveToFirst()
                    fullPath = cursor.getString(cursor.getColumnIndexOrThrow(column))
                }
            } finally {
                if (cursor != null) cursor.close()
            }
        }
        return fullPath
    }
}