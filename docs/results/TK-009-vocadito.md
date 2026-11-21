# TK-009: evaluation of both pitch candidates on vocadito

Produced by `analysis/tools/evaluate_vocadito.py` in the run "Humming evaluation" of the CI service.
Change: `eaf135448f62`. Requirement: NFR-QLT-01. Check: V-05, host part (`technology_stack.md` Section 8.2).

## Data and method

1. Set: vocadito, 40 tracks of solo singing, licence CC-BY-4.0. R. M. Bittner, K. Pasalo, J. J. Bosch, G. Meseguer-Brocal, D. Rubinstein: vocadito, a dataset of solo vocals with f0, note, and lyric annotations. Zenodo record 5578807, 2021.
2. The set was downloaded at run time and checked against its checksum. It is not stored in the repository.
3. Each track was reduced from 44.1 kHz to 16 kHz with `scipy.signal.resample_poly`.
4. Each candidate ran with the same code for Notes and its start value of the confidence (`feature_specifications.md` Section 4.2.4).
5. Metric: `mir_eval.transcription.precision_recall_f1_overlap`, Notes without offsets, 50 ms for the start, 50 cents for the pitch. The Notes keep the sung pitch (no shift by octaves).
6. Each track is scored against annotator A1 and against annotator A2. The table gives the means over the tracks.

## Result

| Candidate | F-measure (A1) | F-measure (A2) | Mean of both | Precision (A1) | Recall (A1) | Time / audio on the host |
|---|---|---|---|---|---|---|
| pyin | 0.653 | 0.628 | 0.641 | 0.741 | 0.593 | 0.0113 |
| swiftf0 | 0.685 | 0.632 | 0.659 | 0.736 | 0.649 | 0.0068 |

Difference of the means, SwiftF0 minus pYIN: +0.018.

This set has no pass value. A published specialist model reaches 0.57 to 0.64 without offsets (`requirements.md` NFR-QLT-01, [S29]).
The rule of V-05 (`technology_stack.md` Section 8.2) also needs the 10 melodies of the owner on the phone (TK-010). This report alone names no winner.
The time on the host is no measurement of the phone (NFR-PERF-09 is measured in TK-010).

## Each track

| Track | pYIN A1 | pYIN A2 | SwiftF0 A1 | SwiftF0 A2 |
|---|---|---|---|---|
| vocadito_1 | 0.761 | 0.780 | 0.750 | 0.768 |
| vocadito_2 | 0.508 | 0.532 | 0.598 | 0.614 |
| vocadito_3 | 0.826 | 0.782 | 0.817 | 0.727 |
| vocadito_4 | 0.462 | 0.581 | 0.598 | 0.566 |
| vocadito_5 | 0.603 | 0.561 | 0.667 | 0.638 |
| vocadito_6 | 0.642 | 0.632 | 0.699 | 0.569 |
| vocadito_7 | 0.822 | 0.682 | 0.723 | 0.652 |
| vocadito_8 | 0.833 | 0.899 | 0.892 | 0.958 |
| vocadito_9 | 0.708 | 0.655 | 0.726 | 0.691 |
| vocadito_10 | 0.552 | 0.520 | 0.508 | 0.364 |
| vocadito_11 | 0.656 | 0.596 | 0.646 | 0.552 |
| vocadito_12 | 0.755 | 0.680 | 0.702 | 0.630 |
| vocadito_13 | 0.425 | 0.419 | 0.500 | 0.377 |
| vocadito_14 | 0.580 | 0.492 | 0.639 | 0.531 |
| vocadito_15 | 0.674 | 0.558 | 0.674 | 0.562 |
| vocadito_16 | 0.831 | 0.564 | 0.831 | 0.538 |
| vocadito_17 | 0.660 | 0.489 | 0.627 | 0.400 |
| vocadito_18 | 0.674 | 0.595 | 0.667 | 0.565 |
| vocadito_19 | 0.542 | 0.576 | 0.562 | 0.514 |
| vocadito_20 | 0.721 | 0.767 | 0.730 | 0.839 |
| vocadito_21 | 0.419 | 0.478 | 0.561 | 0.496 |
| vocadito_22 | 0.693 | 0.753 | 0.718 | 0.725 |
| vocadito_23 | 0.559 | 0.571 | 0.747 | 0.742 |
| vocadito_24 | 0.778 | 0.794 | 0.800 | 0.885 |
| vocadito_25 | 0.727 | 0.639 | 0.633 | 0.568 |
| vocadito_26 | 0.532 | 0.587 | 0.712 | 0.733 |
| vocadito_27 | 0.730 | 0.877 | 0.657 | 0.885 |
| vocadito_28 | 0.532 | 0.564 | 0.643 | 0.602 |
| vocadito_29 | 0.746 | 0.794 | 0.767 | 0.725 |
| vocadito_30 | 0.559 | 0.640 | 0.646 | 0.642 |
| vocadito_31 | 0.659 | 0.658 | 0.690 | 0.693 |
| vocadito_32 | 0.660 | 0.635 | 0.692 | 0.667 |
| vocadito_33 | 0.516 | 0.470 | 0.509 | 0.489 |
| vocadito_34 | 0.489 | 0.414 | 0.548 | 0.454 |
| vocadito_35 | 0.618 | 0.456 | 0.701 | 0.424 |
| vocadito_36 | 0.615 | 0.673 | 0.778 | 0.743 |
| vocadito_37 | 0.800 | 0.659 | 0.821 | 0.682 |
| vocadito_38 | 0.782 | 0.739 | 0.768 | 0.742 |
| vocadito_39 | 0.718 | 0.639 | 0.690 | 0.622 |
| vocadito_40 | 0.762 | 0.720 | 0.771 | 0.703 |
