package com.example.hit_run_car

import android.content.Intent
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_report.*

class reportActivity : AppCompatActivity() {
    lateinit var reportDatetime : String
    lateinit var reportAddress : String
    lateinit var reportBitmap: Bitmap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)

        reportAddress = intent.getStringExtra("address").toString()
        reportDatetime = intent.getStringExtra("datetime").toString()
//        reportBitmap = intent.getExtras()?.get("bitmap") as Bitmap

        Log.d("reportAddress",reportAddress)
        Log.d("reportDatetime",reportDatetime)

//        imageView.setImageBitmap(reportBitmap)
        car_time_textView.text = "시각 : " + reportDatetime
        car_location_textView.text = "위치정보 : " + reportAddress

    }

}