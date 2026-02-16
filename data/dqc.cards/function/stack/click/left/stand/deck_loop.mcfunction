
data modify entity @n[tag=dqc.cards.active_stack] data.cards append from storage dqc.cards:tmp deck[0]
data modify entity @n[tag=dqc.cards.active_stack] data.cards[-1].components."minecraft:custom_model_data".flags[0] set value 0b
data remove storage dqc.cards:tmp deck[0]

playsound minecraft:item.bundle.insert player @a ~ ~ ~ 0.1 1
execute if data storage dqc.cards:tmp deck[0] run function dqc.cards:stack/click/left/stand/deck_loop
