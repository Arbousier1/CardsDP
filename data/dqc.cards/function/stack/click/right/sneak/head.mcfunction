data modify entity @n[tag=dqc.cards.active_stack] data.cards[-1].components."minecraft:profile" set from entity @s SelectedItem.components."minecraft:profile"
data modify entity @n[tag=dqc.cards.active_stack] data.cards[-1].components."minecraft:lore"[0] set from entity @s SelectedItem.components."minecraft:profile".name
execute as @n[tag=dqc.cards.active_stack] run function dqc.cards:stack/update_display

playsound minecraft:item.bundle.insert player @a ~ ~ ~ 1 1
