execute unless data entity @s data.cards[0] run return fail

data modify storage dqc.cards:tmp deck set from entity @s data.cards
execute store result storage dqc.cards:tmp count float 0.0185 run data get storage dqc.cards:tmp deck
data modify entity @s data.cards set value []

execute positioned ~ ~0.125 ~ run loot spawn ~ ~ ~ loot dqc.cards:deck_empty
execute positioned ~ ~0.125 ~ as @n[type=item] run function dqc.cards:deck/item_set_m with storage dqc.cards:tmp

tag @s add dqc.cards.active_stack
execute as @n[type=interaction] run function dqc.cards:stack/update_display
tag @s remove dqc.cards.active_stack