# Sanitize the card
data modify entity @s data.cards[0].components."minecraft:custom_model_data".flags[0] set value 0b

data modify storage dqc.cards:tmp deck append from entity @s data.cards[0]

data remove entity @s data.cards[0]
execute if data entity @s data.cards[0] run function dqc.cards:stack/click/left/stand/empty_loop