# If our offhand is empty, add a tag to the player then copy the card over
execute unless items entity @s weapon.offhand * run tag @s add dqc.cards.sneak_grab
execute unless items entity @s weapon.offhand * run item replace entity @s weapon.offhand from entity @n[tag=dqc.cards.active_stack] container.0

# If the offhand is a single card, convert it to a deck:
execute if items entity @s weapon.offhand *[custom_data~{dqc.cards:{is_card:true}}] run function dqc.cards:deck/offhand_convert_to_deck

# If we had the tag, clear it and return now
execute if entity @s[tag=dqc.cards.sneak_grab] run return run tag @s remove dqc.cards.sneak_grab

# Add the card in the stack to the offhand deck
data modify storage dqc.cards:tmp deck set from entity @s equipment.offhand.components."minecraft:bundle_contents"
data modify storage dqc.cards:tmp deck prepend from entity @n[tag=dqc.cards.active_stack] data.cards[-1]

# Save the card count for setting damage later
execute store result storage dqc.cards:tmp count float 0.0185 run data get storage dqc.cards:tmp deck
execute store result storage dqc.cards:tmp color int 1 run data get storage dqc.cards:tmp deck[0].components."minecraft:custom_model_data".colors[1]

function dqc.cards:deck/offhand_set_m with storage dqc.cards:tmp
