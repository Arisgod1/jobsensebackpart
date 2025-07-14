import json
import time

import cv2
import dlib
import imutils
import win32con
import win32gui

from parameters import NETWORK, DATASET, VIDEO_PREDICTOR
from predict import load_model, predict


class EmotionRecognizer:
    """
    表情识别类，基于摄像头实时识别人脸表情，并统计出现次数。
    """

    BOX_COLOR = (250,250,210)
    TEXT_COLOR = (250,250,210)
    WINDOW_TITLE = "Interview started,Direct view camera"

    def __init__(self):
        # 初始化视频流
        self.video_stream = cv2.VideoCapture(VIDEO_PREDICTOR.camera_source)

        # 加载人脸检测分类器
        self.face_detector = cv2.CascadeClassifier(VIDEO_PREDICTOR.face_detection_classifier)

        # 条件加载dlib关键点检测器
        self.shape_predictor = None
        if NETWORK.use_landmarks:
            self.shape_predictor = dlib.shape_predictor(DATASET.shape_predictor_path)

        # 加载表情识别模型
        self.model = load_model()

        # 初始化预测状态
        self.last_predicted_time = 0
        self.last_predicted_confidence = 0
        self.last_predicted_emotion = ""

        # 初始化情绪计数器，确保每个情绪键都有
        self.emotion_counts = {emotion: 0 for emotion in VIDEO_PREDICTOR.emotions}

    def predict_emotion(self, image):
        """
        预测图像表情及置信度。
        :param image: 灰度人脸图像
        :return: (emotion_label, confidence)
        """
        image.resize([NETWORK.input_size, NETWORK.input_size], refcheck=False)
        emotion, confidence = predict(image, self.model, self.shape_predictor)
        return emotion, confidence

    def _draw_label(self, frame, label, confidence, x, y):
        """
        绘制表情标签和置信度到视频帧。
        """
        if VIDEO_PREDICTOR.show_confidence and confidence is not None:
            text = f"{label} ({confidence * 100:.1f}%)"
        else:
            text = label

        if label:
            cv2.putText(frame, text, (x - 20, y - 20),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.7, self.TEXT_COLOR, 2)

    def recognize_emotions(self):
        """
        实时识别摄像头画面中的表情，显示并统计。
        """
        failed_frames_count = 0
        start_time = time.time()
        try:
            while True:
                if time.time() - start_time > 180:
                    print("时间到,面试结束!")
                    break
                grabbed, frame = self.video_stream.read()

                if not grabbed:
                    failed_frames_count += 1
                    if failed_frames_count > 10:
                        print("无法捕获视频帧，程序退出。")
                        break
                    continue  # 跳过本次循环，等待下一帧

                failed_frames_count = 0  # 成功获取一帧，计数清零

                # 调整图像宽度
                frame = imutils.resize(frame, width=600)
                gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)

                # 检测人脸
                faces = self.face_detector.detectMultiScale(gray, 1.3, 5)

                current_time = time.time()

                for (x, y, w, h) in faces:
                    # 过滤过小的检测结果
                    if w < 30 and h < 30:
                        continue

                    # 画人脸框
                    cv2.rectangle(frame, (x, y), (x + w, y + h), self.BOX_COLOR, 2)

                    # 抠出人脸区域用于识别
                    face = gray[y:y + h, x:x + w].copy()

                    # 是否等待时间间隔以复用上次预测结果
                    if (current_time - self.last_predicted_time) < VIDEO_PREDICTOR.time_to_wait_between_predictions:
                        label = self.last_predicted_emotion
                        confidence = self.last_predicted_confidence
                    else:
                        label, confidence = self.predict_emotion(face)
                        self.last_predicted_emotion = label
                        self.last_predicted_confidence = confidence
                        self.last_predicted_time = current_time

                        # 更新统计计数
                        if label in self.emotion_counts:
                            self.emotion_counts[label] += 1
                        else:
                            self.emotion_counts[label] = 1

                    # 绘制识别标签
                    self._draw_label(frame, label, confidence, x, y)

                # 显示结果窗口
                cv2.imshow(self.WINDOW_TITLE, frame)
                cv2.waitKey(1)  # 确保窗口创建完成

                # 检测窗口是否被关闭
                if cv2.getWindowProperty(self.WINDOW_TITLE, cv2.WND_PROP_VISIBLE) < 1:
                    print("检测到窗口已关闭，程序退出。")
                    break

                # 获取窗口句柄
                hwnd = win32gui.FindWindow(None, "Interview started,Direct view camera")
                if hwnd:
                    # 设置窗口置顶
                    win32gui.SetWindowPos(hwnd, win32con.HWND_TOPMOST,
                                          0, 0, 0, 0,
                                          win32con.SWP_NOMOVE | win32con.SWP_NOSIZE)
                # 按'q'键退出
                if cv2.waitKey(1) & 0xFF == ord('q'):
                    break
        except Exception as e:
            print(f"发生异常: {e}")

        finally:
            # 程序退出时保存表情统计结果
            output_json_path = "emotion_counts.json"
            with open(output_json_path, "w", encoding='utf-8') as f:
                json.dump(self.emotion_counts, f, ensure_ascii=False, indent=4)
            print(f"表情统计已保存到 {output_json_path}")

            # 释放资源
            self.video_stream.release()
            cv2.destroyAllWindows()


if __name__ == "__main__":
    recognizer = EmotionRecognizer()
    recognizer.recognize_emotions()
