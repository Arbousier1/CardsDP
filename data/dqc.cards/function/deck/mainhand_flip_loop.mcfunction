
data modify storage dqc.cards:tmp deck append from storage dqc.cards:tmp deck_f[-1]
data remove storage dqc.cards:tmp deck_f[-1]

execute if data storage dqc.cards:tmp deck_f[-1] run function dqc.cards:deck/mainhand_flip_loop