package com.example.hit_run_car

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_report.*

class reportActivity : AppCompatActivity() {
    lateinit var reportDatetime : String
    lateinit var reportaddress : String
    lateinit var reportResult : String
    var reportDegree : Int = 0
    lateinit var currentPhotoPath : String

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

        val bitmap = BitmapFactory.decodeFile(currentPhotoPath)
        imageView2.setImageBitmap(rotate(bitmap, reportDegree))
    }

    private fun rotate(bitmap: Bitmap, degree: Int): Bitmap? {
        Log.d("rotate","init rotate")
        val matrix = Matrix()
        matrix.postRotate(degree.toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix,true)
    }
}
