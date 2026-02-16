advancement revoke @s only dqc.cards:stack_left

tag @s add dqc.cards.active_player
execute as @n[tag=dqc.cards.stack,nbt={attack:{}}] on vehicle run tag @s add dqc.cards.active_stack

function dqc.cards:stack/click/left

tag @p[tag=dqc.cards.active_player] remove dqc.cards.active_player
tag @n[tag=dqc.cards.active_stack] remove dqc.cards.active_stack

data remove entity @n[tag=dqc.cards.stack,nbt={attack:{}}] attack