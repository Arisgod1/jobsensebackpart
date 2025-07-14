from tflearn.layers.conv import conv_2d, max_pool_2d
from tflearn.layers.core import input_data, dropout, fully_connected
from tflearn.layers.estimator import regression
from tflearn.layers.merge_ops import merge
from tflearn.layers.normalization import batch_normalization
from tflearn.optimizers import Momentum, Adam

from parameters import NETWORK, HYPERPARAMS


def conv_block(input_layer, filters, kernel_size, use_batchnorm=True, activation='relu', pool_size=3, pool_stride=2):
    layer = conv_2d(input_layer, filters, kernel_size, activation=activation)
    if use_batchnorm:
        layer = batch_normalization(layer)
    layer = max_pool_2d(layer, pool_size, strides=pool_stride)
    return layer


def fc_block(input_layer, units, use_batchnorm=True, activation='relu', keep_prob=None):
    layer = fully_connected(input_layer, units, activation=activation)
    if use_batchnorm:
        layer = batch_normalization(layer)
    if keep_prob is not None:
        layer = dropout(layer, keep_prob=keep_prob)
    return layer


def get_optimizer(name, optimizer_param, learning_rate, learning_rate_decay, decay_step):
    optimizers = {
        'momentum': Momentum(learning_rate=learning_rate, momentum=optimizer_param,
                             lr_decay=learning_rate_decay, decay_step=decay_step),
        'adam': Adam(learning_rate=learning_rate, beta1=optimizer_param, beta2=learning_rate_decay)
    }
    opt = optimizers.get(name)
    if opt is None:
        raise ValueError(f"Unknown optimizer: {name}")
    return opt


def build_model(optimizer=HYPERPARAMS.optimizer,
                optimizer_param=HYPERPARAMS.optimizer_param,
                learning_rate=HYPERPARAMS.learning_rate,
                keep_prob=HYPERPARAMS.keep_prob,
                learning_rate_decay=HYPERPARAMS.learning_rate_decay,
                decay_step=HYPERPARAMS.decay_step):

    if NETWORK.model == 'A':
        return build_modelA(optimizer, optimizer_param, learning_rate, keep_prob, learning_rate_decay, decay_step)
    elif NETWORK.model == 'B':
        return build_modelB(optimizer, optimizer_param, learning_rate, keep_prob, learning_rate_decay, decay_step)
    else:
        raise ValueError(f"ERROR: unknown model {NETWORK.model}")


def build_modelB(optimizer, optimizer_param, learning_rate, keep_prob, learning_rate_decay, decay_step):
    input_img = input_data(shape=[None, NETWORK.input_size, NETWORK.input_size, 1], name='input1')

    x = conv_block(input_img, 64, 3, use_batchnorm=NETWORK.use_batchnorm_after_conv_layers, activation=NETWORK.activation)
    x = conv_block(x, 128, 3, use_batchnorm=NETWORK.use_batchnorm_after_conv_layers, activation=NETWORK.activation)
    x = conv_block(x, 256, 3, use_batchnorm=NETWORK.use_batchnorm_after_conv_layers, activation=NETWORK.activation)
    x = dropout(x, keep_prob=keep_prob)
    x = fc_block(x, 4096, use_batchnorm=False, activation=NETWORK.activation, keep_prob=keep_prob)
    x = fc_block(x, 1024, use_batchnorm=NETWORK.use_batchnorm_after_fully_connected_layers, activation=NETWORK.activation)

    if NETWORK.use_landmarks or NETWORK.use_hog_and_landmarks:
        if NETWORK.use_hog_sliding_window_and_landmarks:
            landmarks_input = input_data(shape=[None, 2728], name='input2')
        elif NETWORK.use_hog_and_landmarks:
            landmarks_input = input_data(shape=[None, 208], name='input2')
        else:
            landmarks_input = input_data(shape=[None, 68, 2], name='input2')

        landmarks_net = fc_block(landmarks_input, 1024, NETWORK.use_batchnorm_after_fully_connected_layers, NETWORK.activation)
        landmarks_net = fc_block(landmarks_net, 128, NETWORK.use_batchnorm_after_fully_connected_layers, NETWORK.activation)
        x = fc_block(x, 128, use_batchnorm=False, activation=NETWORK.activation)
        network = merge([x, landmarks_net], 'concat', axis=1)
    else:
        network = x

    output = fully_connected(network, NETWORK.output_size, activation='softmax')

    opt = get_optimizer(optimizer, optimizer_param, learning_rate, learning_rate_decay, decay_step)

    network = regression(output, optimizer=opt, loss=NETWORK.loss, learning_rate=learning_rate, name='output')
    return network


def build_modelA(optimizer, optimizer_param, learning_rate, keep_prob, learning_rate_decay, decay_step):
    input_img = input_data(shape=[None, NETWORK.input_size, NETWORK.input_size, 1], name='input1')

    x = conv_block(input_img, 64, 5, use_batchnorm=NETWORK.use_batchnorm_after_conv_layers, activation=NETWORK.activation)
    x = conv_block(x, 64, 5, use_batchnorm=NETWORK.use_batchnorm_after_conv_layers, activation=NETWORK.activation)
    x = conv_2d(x, 128, 4, activation=NETWORK.activation)
    if NETWORK.use_batchnorm_after_conv_layers:
        x = batch_normalization(x)
    x = dropout(x, keep_prob=keep_prob)
    x = fc_block(x, 1024, use_batchnorm=NETWORK.use_batchnorm_after_fully_connected_layers, activation=NETWORK.activation)

    if NETWORK.use_landmarks or NETWORK.use_hog_and_landmarks:
        if NETWORK.use_hog_sliding_window_and_landmarks:
            landmarks_input = input_data(shape=[None, 2728], name='input2')
        elif NETWORK.use_hog_and_landmarks:
            landmarks_input = input_data(shape=[None, 208], name='input2')
        else:
            landmarks_input = input_data(shape=[None, 68, 2], name='input2')

        landmarks_net = fc_block(landmarks_input, 1024, NETWORK.use_batchnorm_after_fully_connected_layers, NETWORK.activation)
        landmarks_net = fc_block(landmarks_net, 40, NETWORK.use_batchnorm_after_fully_connected_layers, NETWORK.activation)
        x = fc_block(x, 40, use_batchnorm=False, activation=NETWORK.activation)
        network = merge([x, landmarks_net], 'concat', axis=1)
    else:
        network = x

    output = fully_connected(network, NETWORK.output_size, activation='softmax')

    opt = get_optimizer(optimizer, optimizer_param, learning_rate, learning_rate_decay, decay_step)

    network = regression(output, optimizer=opt, loss=NETWORK.loss, learning_rate=learning_rate, name='output')
    return network
