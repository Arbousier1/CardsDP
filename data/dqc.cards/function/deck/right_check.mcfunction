execute as @a[scores={dqc.cards.use_fall=1}] run function dqc.cards:deck/right_fall
execute as @a[scores={dqc.cards.use_fall=1..}] run scoreboard players remove @s dqc.cards.use_fall 1
execute if entity @p[scores={dqc.cards.use_fall=1..}] run schedule function dqc.cards:deck/right_check 1t append