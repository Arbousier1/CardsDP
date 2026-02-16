execute as @n[tag=dqc.cards.active_stack] if entity @s[nbt={item:{components:{"minecraft:item_model":"dqc.cards:empty_slot","minecraft:custom_model_data":{flags:[true]}}}}] run return fail

data modify entity @n[tag=dqc.cards.active_stack] data.cards append from entity @s SelectedItem
data modify entity @n[tag=dqc.cards.active_stack] data.cards[-1].components."minecraft:custom_model_data".flags[0] set value 1b
data modify entity @n[tag=dqc.cards.active_stack] data.cards[-1].count set value 1
execute as @n[tag=dqc.cards.active_stack] run function dqc.cards:stack/update_display

item modify entity @s weapon.mainhand {function:set_count,count:-1,add:true}

playsound minecraft:item.bundle.insert player @a ~ ~ ~ 1 1