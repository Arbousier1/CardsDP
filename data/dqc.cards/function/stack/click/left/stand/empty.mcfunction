# If this stack is empty, toggle it's visibility/usability
execute as @n[tag=dqc.cards.active_stack] unless data entity @s data.cards[0] run return run function dqc.cards:stack/visibility_toggle

# Copy the cards to storage
data modify storage dqc.cards:tmp deck set value []
execute as @n[tag=dqc.cards.active_stack] run function dqc.cards:stack/click/left/stand/empty_loop
execute as @n[tag=dqc.cards.active_stack] run function dqc.cards:stack/update_display

# Give the player an empty deck
loot replace entity @s weapon.mainhand loot dqc.cards:deck_empty
execute store result storage dqc.cards:tmp count float 0.0185 run data get storage dqc.cards:tmp deck
execute store result storage dqc.cards:tmp color int 1 run data get storage dqc.cards:tmp deck[0].components."minecraft:custom_model_data".colors[1]
function dqc.cards:deck/mainhand_set_m with storage dqc.cards:tmp