# Fail out if the card slot is disabled
execute as @n[tag=dqc.cards.active_stack] if entity @s[nbt={item:{components:{"minecraft:item_model":"dqc.cards:empty_slot","minecraft:custom_model_data":{flags:[true]}}}}] run return fail

# Copy card data from hand to storage
data modify storage dqc.cards:tmp deck set from entity @s SelectedItem.components."minecraft:bundle_contents"
function dqc.cards:stack/click/left/sneak/deck_loop

execute as @n[tag=dqc.cards.active_stack] run function dqc.cards:stack/update_display

item replace entity @s weapon.mainhand with minecraft:air