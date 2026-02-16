# Swaps element i with element j
$data modify storage dqc.cards:tmp tmp set from storage dqc.cards:tmp deck[$(i)]
$data modify storage dqc.cards:tmp deck[$(i)] set from storage dqc.cards:tmp deck[$(j)]
$data modify storage dqc.cards:tmp deck[$(j)] set from storage dqc.cards:tmp tmp