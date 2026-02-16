# If this stack is empty, toggle it's visibility/usability
execute as @n[tag=dqc.cards.active_stack] unless data entity @s data.cards[0] run return run function dqc.cards:stack/visibility_toggle

# If we can't add the card to the offhand, fail out
execute if items entity @s weapon.offhand * unless items entity @s weapon.offhand *[custom_data~{dqc.cards:{is_deck:true}}|minecraft:custom_data~{dqc.cards:{is_card:true}}] run return fail

# If the player's offhand is empty, give them an empty deck
execute unless items entity @s weapon.offhand * run loot replace entity @s weapon.offhand loot dqc.cards:deck_empty

# If the player's offhand is a card, convert it to a deck
execute if items entity @s weapon.offhand *[minecraft:custom_data~{dqc.cards:{is_card:true}}] run function dqc.cards:deck/offhand_convert_to_deck

# Copy the player's offhand deck to storage
data modify storage dqc.cards:tmp deck set from entity @s equipment.offhand.components."minecraft:bundle_contents"

# Copy the cards from the table to storage
execute as @n[tag=dqc.cards.active_stack] run function dqc.cards:stack/click/left/sneak/empty_loop
execute as @n[tag=dqc.cards.active_stack] run function dqc.cards:stack/update_display

# Give the player an empty deck
execute store result storage dqc.cards:tmp count float 0.0185 run data get storage dqc.cards:tmp deck
execute store result storage dqc.cards:tmp color int 1 run data get storage dqc.cards:tmp deck[0].components."minecraft:custom_model_data".colors[1]
function dqc.cards:deck/offhand_set_m with storage dqc.cards:tmp