package com.example.safewalknav_test

import org.opencv.android.CameraBridgeViewBase
import org.opencv.core.*
import org.opencv.video.Video
import org.opencv.imgproc.Imgproc

//Optical Flow 기반 영상 분석
// CameraBridgeViewBase에서 전달되는 프레임 처리
class OpticalFlowAnalyzer : CameraBridgeViewBase.CvCameraViewListener2 {

    //이전 프레임의 grayscale 이미지
    private lateinit var prevGray: Mat

    // 이전 프레임의 특징점
    private lateinit var prevPts: MatOfPoint2f

    //현재 프레임에서 계산된 특징점
    private lateinit var nextPts: MatOfPoint2f

    //각 특징점 추적 성공 여부
    private lateinit var status: MatOfByte

    //각 특징점 추적 오차값
    private lateinit var err: MatOfFloat

    //카메라 시작 시 한 번 호출
    override fun onCameraViewStarted(width: Int, height: Int) {
        prevGray = Mat()
        prevPts = MatOfPoint2f()
        nextPts = MatOfPoint2f()
        status = MatOfByte()
        err = MatOfFloat()
    }

    //매 프레임마다 호출되는 핵심 함수
    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame): Mat {
        //초기화 안 된 경우 원본 그대로 반환
        if (!::prevGray.isInitialized) return inputFrame.rgba()
        //컬러 영상
        val rgba = inputFrame.rgba()
        // grayscale 영상
        val currentGray = inputFrame.gray()

        //첫 프레임 처리
        if (prevGray.empty()) {
            currentGray.copyTo(prevGray)
            // 추적할 특징점 초기 추출
            val tempCorners = MatOfPoint() //추적 특징점 초기 추출
            Imgproc.goodFeaturesToTrack(prevGray, tempCorners, 100, 0.3, 7.0)
            tempCorners.convertTo(prevPts, CvType.CV_32F) // 실수형으로 변환
            tempCorners.release() // 메모리 해제
            return rgba
        }

        // Optical Flow 계산
        if (!prevPts.empty()) {
            Video.calcOpticalFlowPyrLK(prevGray, currentGray, prevPts, nextPts, status, err)

            val statusArray = status.toArray()
            val prevList = prevPts.toList()
            val nextList = nextPts.toList()
            //유효한 특징점만 저장
            val goodPts = mutableListOf<Point>()

            for (i in statusArray.indices) {
                if (statusArray[i].toInt() == 1) {
                    val p1 = prevList[i] //이전 위치
                    val p2 = nextList[i] // 현재 위치

                    // 움직임 선 그리기 (초록색)
                    Imgproc.line(rgba, p1, p2, Scalar(0.0, 255.0, 0.0), 2)
                    // 현재 지점 점 찍기 (빨간색)
                    Imgproc.circle(rgba, p2, 5, Scalar(255.0, 0.0, 0.0), -1)

                    goodPts.add(p2)
                }
            }
            //유효한 특징점만 다음 프레임 기준으로 사용
            prevPts.fromList(goodPts)
        }
        //현재 프레임을 이전 프레임으로 저장
        currentGray.copyTo(prevGray)

        // 특징점이 부족해지면 다시 찾기
        if (prevPts.rows() < 20) {
            val tempCorners = MatOfPoint()
            Imgproc.goodFeaturesToTrack(prevGray, tempCorners, 100, 0.3, 7.0)
            tempCorners.convertTo(prevPts, CvType.CV_32F) // 실수형으로 변환
            tempCorners.release() // 메모리 해제
        }

        //시각화 결과 반환
        return rgba
    }

    //카메라 종료시 메모리 해제
    override fun onCameraViewStopped() {
        prevGray.release()
    }
}