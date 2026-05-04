package com.example.safewalknav_test

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.OpenCVLoader
import android.util.Log

class MainActivity : AppCompatActivity() {

    // OpenCV 카메라 뷰
    private lateinit var cameraView: CameraBridgeViewBase
    // Optical Flow 분석
    private lateinit var analyzer: OpticalFlowAnalyzer
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //레이아웃 설정
        setContentView(R.layout.activity_main)
        //카메라 뷰 초기화
        cameraView = findViewById(R.id.opencv_camera_view)
        //Optical Flow 분석 객체 생성
        analyzer = OpticalFlowAnalyzer()
        //카메라 프레임이 들어올 때 analyzer가 처리
        cameraView.setCvCameraViewListener(analyzer)
        // OpenCV 네이티브 라이브러리 초기화
        if (!OpenCVLoader.initDebug()) {
            // 초기화 실패 시 로그 출력 후 종료 (카메라 실행 불가)
            Log.e("OpenCV", "OpenCV init failed")
            return
        }

        //카메라 권한 확인
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            //권한이 이미 있는 경우
            cameraView.setCameraPermissionGranted()
        } else {
            // 권한이 없는 경우 사용자에게 요청
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)
        }
    }

    // 사용자에게 권한 요청 후 결과를 받는 콜백 함수
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        //카메라 권한 요청 결과 처리
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            //권한 허용 시 카메라 활성화
            cameraView.setCameraPermissionGranted()
            cameraView.enableView()
        }
    }

    override fun onResume() {
        super.onResume()
        //Activity가 다시 활성화될 때 카메라 재시작
        if (::cameraView.isInitialized &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED &&
            OpenCVLoader.initDebug()
        ) {
            cameraView.setCameraPermissionGranted()
            cameraView.enableView()
        }
    }

    override fun onPause() {
        super.onPause()
        //Activity가 백그라운로 갈 때 카메라 해제
        if (::cameraView.isInitialized) cameraView.disableView()
    }

    override fun onDestroy() {
        super.onDestroy()
        //Activity 종료 시 카메라 완전히 해제
        if (::cameraView.isInitialized) cameraView.disableView()
    }
}