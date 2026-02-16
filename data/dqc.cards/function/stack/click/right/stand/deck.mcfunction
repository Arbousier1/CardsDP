# Fail out if the card slot is disabled
execute as @n[tag=dqc.cards.active_stack] if entity @s[nbt={item:{components:{"minecraft:item_model":"dqc.cards:empty_slot","minecraft:custom_model_data":{flags:[true]}}}}] run return fail

data modify entity @n[tag=dqc.cards.active_stack] data.cards append from entity @s SelectedItem.components."minecraft:bundle_contents"[0]
data modify entity @n[tag=dqc.cards.active_stack] data.cards[-1].components."minecraft:custom_model_data".flags[0] set value 0b
execute as @n[tag=dqc.cards.active_stack] run function dqc.cards:stack/update_display

function dqc.cards:deck/mainhand_pop

playsound minecraft:item.bundle.insert player @a ~ ~ ~ 1 1