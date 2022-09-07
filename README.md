# Deep-EOS UIMA

[TextImager](https://github.com/texttechnologylab/textimager-uima) annotator wrapper around [General-Purpose Neural Networks for Sentence Boundary Detection](https://github.com/dbmdz/deep-eos) (`deep-eos`)

[![Conference](http://img.shields.io/badge/conference-KONVENS_2019-4b44ce.svg)](https://konvens.org/proceedings/2019/)
[![Paper](http://img.shields.io/badge/paper-KONVENS_Proceedings-B31B1B.svg)](https://konvens.org/proceedings/2019/papers/KONVENS2019_paper_41.pdf)
[![version](https://img.shields.io/github/license/texttechnologylab/deep-eos-uima)]()
[![latest](https://img.shields.io/github/v/release/texttechnologylab/deep-eos-uima)]()

## Description

Uses [Jep](https://github.com/ninia/jep) to load and execute the original `deep-eos` TensorFlow code from [Stefan Schweter](https://github.com/stefan-it).
The model included in this repository was also published with the original `deep-eos` code (find the models [here](https://github.com/dbmdz/deep-eos#development-set)).

## Citation

If you want to use this project, please cite:

> S. Schweter and S. Ahmed _and M. Stoeckel_[^1], "Deep-EOS: General-Purpose Neural Networks for Sentence Boundary Detection” in Proceedings of the 15th Conference on Natural Language Processing (KONVENS), 2019.

### BibTeX

```
@inproceedings{schweter-ahmed-2019-deep,
    author = "Stefan Schweter and Sajawel Ahmed",
    title = "Deep-EOS: General-Purpose Neural Networks for Sentence Boundary Detection",
    booktitle = "Proceedings of the 15th Conference on Natural Language Processing (KONVENS 2019): Short Papers",
    year = "2019",
    address = "Erlangen, Germany",
    publisher = "German Society for Computational Linguistics \& Language Technology",
    pages = "251--255"
}
```

[^1]: erroneously uncredited in the KONVENS paper.
