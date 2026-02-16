data modify storage dqc.cards:tmp deck_f set from entity @s SelectedItem.components."minecraft:bundle_contents"
data modify storage dqc.cards:tmp deck set value []

execute if data storage dqc.cards:tmp deck_f[-1] run function dqc.cards:deck/mainhand_flip_loop

execute store result storage dqc.cards:tmp count float 0.0185 run data get storage dqc.cards:tmp deck
execute store result storage dqc.cards:tmp color int 1 run data get storage dqc.cards:tmp deck[0].components."minecraft:custom_model_data".colors[1]
function dqc.cards:deck/mainhand_set_m with storage dqc.cards:tmp