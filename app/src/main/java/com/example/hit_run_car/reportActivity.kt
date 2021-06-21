package com.example.hit_run_car

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import kotlinx.android.synthetic.main.activity_report.*
import java.io.File


class reportActivity : AppCompatActivity() {
    lateinit var reportDatetime : String
    lateinit var reportaddress : String
    lateinit var reportResult : String
    var reportDegree : Int = 0
    lateinit var currentPhotoPath : String
    var currentImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)

        reportDatetime = intent.getStringExtra("datetime").toString()
        reportaddress = intent.getStringExtra("address").toString()
        reportResult = intent.getStringExtra("result").toString()
        reportDegree = intent.extras?.get("degree") as Int
        currentPhotoPath = intent.getStringExtra("path").toString()

        car_time_textView.text = "시각 : " + reportDatetime
        car_location_textView.text = "위치정보 : " + reportaddress
        car_type_textView.text = "차종 : " + reportResult
        println(currentPhotoPath)
        println("here????")
        val bitmap = BitmapFactory.decodeFile(currentPhotoPath)
        imageView2.setImageBitmap(rotate(bitmap, reportDegree))

        currentImageUri = getUriFromPath(currentPhotoPath)

        btn_report.setOnClickListener{
            if(currentImageUri != null){
                sendMmsIntent()
            }
        }
    }

    private fun sendMmsIntent() {
        try{
            val sendIntent = Intent(Intent.ACTION_SEND)
            sendIntent.putExtra("exit_on_sent",true)
            sendIntent.putExtra(Intent.EXTRA_STREAM,currentImageUri)
            sendIntent.type = "image/*"
            sendIntent.putExtra(Intent.EXTRA_TEXT,"차종 : " + reportResult + "\n시각 : " + reportDatetime + "\n위치정보 : " + reportaddress + "\n해당차량 도주중.")
            startActivity(sendIntent)
        }catch(e: Exception){
            e.printStackTrace()
        }
    }

    private fun rotate(bitmap: Bitmap, degree: Int): Bitmap? {
        Log.d("rotate","init rotate")
        val matrix = Matrix()
        matrix.postRotate(degree.toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix,true)
    }

    private fun getUriFromPath(filePath: String): Uri? {
        val file: File = File(filePath)
        return file.toUri()
    }

}
