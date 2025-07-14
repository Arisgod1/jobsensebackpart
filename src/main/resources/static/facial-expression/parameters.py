import os
from typing import List, Dict

from dataclasses import dataclass, field


@dataclass
class Dataset:
    name: str = 'Fer2013'
    train_folder: str = 'fer2013_features/Training'
    validation_folder: str = 'fer2013_features/PublicTest'
    test_folder: str = 'fer2013_features/PrivateTest'
    shape_predictor_path: str = 'shape_predictor_68_face_landmarks.dat'
    trunc_trainset_to: int = -1  # -1代表全部使用
    trunc_validationset_to: int = -1
    trunc_testset_to: int = -1

@dataclass
class Network:
    model: str = 'B'
    input_size: int = 48
    output_size: int = 7
    activation: str = 'relu'
    loss: str = 'categorical_crossentropy'
    use_landmarks: bool = True
    use_hog_and_landmarks: bool = True
    use_hog_sliding_window_and_landmarks: bool = True
    use_batchnorm_after_conv_layers: bool = True
    use_batchnorm_after_fully_connected_layers: bool = False

@dataclass
class Hyperparams:
    keep_prob: float = 0.956   # dropout = 1 - keep_prob
    learning_rate: float = 0.016
    learning_rate_decay: float = 0.864
    decay_step: int = 50
    optimizer: str = 'momentum'  # {'momentum', 'adam', 'rmsprop', 'adagrad', 'adadelta'}
    optimizer_param: float = 0.95   # momentum值或Adam的beta1

@dataclass
class Training:
    batch_size: int = 128
    epochs: int = 13
    snapshot_step: int = 500
    visualize: bool = True
    logs_dir: str = "logs"
    checkpoint_dir: str = "checkpoints/chk"
    best_checkpoint_path: str = "checkpoints/best/"
    max_checkpoints: int = 1
    checkpoint_frequency: float = 1.0  # 单位：小时
    save_model: bool = True
    save_model_path: str = "best_model/saved_model.bin"

@dataclass
class VideoPredictor:
    emotions: List[str] = field(default_factory=lambda: ["Angry", "Disgust", "Fear", "Happy", "Sad", "Surprise", "Neutral"])
    print_emotions: bool = False
    camera_source: int = 0
    face_detection_classifier: str = "lbpcascade_frontalface.xml"
    show_confidence: bool = False
    time_to_wait_between_predictions: float = 0.5

@dataclass
class OptimizerSearchSpace:
    learning_rate: Dict[str, float] = field(default_factory=lambda: {'min': 0.00001, 'max': 0.1})
    learning_rate_decay: Dict[str, float] = field(default_factory=lambda: {'min': 0.5, 'max': 0.99})
    optimizer: List[str] = field(default_factory=lambda: ['momentum'])
    optimizer_param: Dict[str, float] = field(default_factory=lambda: {'min': 0.5, 'max': 0.99})
    keep_prob: Dict[str, float] = field(default_factory=lambda: {'min': 0.7, 'max': 0.99})

def make_dir(folder: str) -> None:
    os.makedirs(folder, exist_ok=True)

# 实例化配置对象
DATASET = Dataset()
NETWORK = Network()
TRAINING = Training()
HYPERPARAMS = Hyperparams()
VIDEO_PREDICTOR = VideoPredictor()
OPTIMIZER = OptimizerSearchSpace()

# 创建必要目录
make_dir(TRAINING.logs_dir)
make_dir(TRAINING.checkpoint_dir)
