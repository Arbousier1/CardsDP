# If this stack is empty, toggle it's visibility/usability
execute as @n[tag=dqc.cards.active_stack] unless data entity @s data.cards[0] run return run function dqc.cards:stack/visibility_set

# Sanitize the card before giving it to the player
data modify entity @n[tag=dqc.cards.active_stack] item.components."minecraft:custom_model_data".flags[0] set value 0b
item replace entity @s weapon.mainhand from entity @n[tag=dqc.cards.active_stack] container.0

data remove entity @n[tag=dqc.cards.active_stack] data.cards[-1]

execute as @n[tag=dqc.cards.active_stack] run function dqc.cards:stack/update_display

playsound minecraft:item.bundle.remove_one player @a ~ ~ ~ 1 1