advancement revoke @s only dqc.cards:table_left

tag @s add dqc.cards.active_player

# Damage the table, then remove the attacker data
execute as @n[tag=dqc.cards.table,nbt={attack:{}}] at @s run function dqc.cards:table/damage
data remove entity @n[tag=dqc.cards.table,nbt={attack:{}}] attack

tag @s remove dqc.cards.active_player