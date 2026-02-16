advancement revoke @s only dqc.cards:stack_right

tag @s add dqc.cards.active_player
execute as @n[tag=dqc.cards.stack,nbt={interaction:{}}] on vehicle run tag @s add dqc.cards.active_stack

function dqc.cards:stack/click/right

tag @s remove dqc.cards.active_player
tag @n[tag=dqc.cards.active_stack] remove dqc.cards.active_stack

data remove entity @n[tag=dqc.cards.stack,nbt={interaction:{}}] interaction