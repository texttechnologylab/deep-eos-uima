import os
from typing import List

from utils import Utils

os.environ['TF_CPP_MIN_LOG_LEVEL'] = '2'
os.environ['CUDA_VISIBLE_DEVICES'] = ''
import tensorflow as tf
from keras.models import load_model

util = Utils()


def print_flag(content, big=True):
    l = len(content)
    bar = '#' * (8 + l)
    if big:
        print(f'{bar}\n'
              f'### {content} ###\n'
              f'{bar}')
    else:
        print(f'### {content} ###')


class DeepEosModel:
    def __init__(self, model_base_path, window_size=4, batch_size=32):
        print_flag('Loading deep-eos model')
        self.char_2_id_dict = util.load_vocab(model_base_path + ".vocab")
        if os.path.exists(model_base_path + ".hd5f"):
            self.deep_eos_model = load_model(model_base_path + ".hd5f")
        else:
            self.deep_eos_model = load_model(model_base_path + ".model")
        self.deep_eos_graph = tf.get_default_graph()
        self.window_size = window_size
        self.batch_size = batch_size
        print(self.deep_eos_model.summary())

    def tag(self, text) -> List[int]:
        potential_eos_list = util.build_potential_eos_list(text, self.window_size)

        eos_pos = []
        for potential_eos in potential_eos_list:
            eos_position, char_sequence = potential_eos
            data_set = util.build_data_set([(-1.0, char_sequence)], self.char_2_id_dict, self.window_size)

            if len(data_set) > 0:
                label, feature_vector = data_set[0]

                with self.deep_eos_graph.as_default():
                    predicted = self.deep_eos_model.predict(
                        feature_vector.reshape(1, 2 * self.window_size + 1),
                        batch_size=self.batch_size,
                        verbose=0)

                    if predicted[0][0] >= 0.5:
                        eos_pos.append(int(eos_position))

        return eos_pos
