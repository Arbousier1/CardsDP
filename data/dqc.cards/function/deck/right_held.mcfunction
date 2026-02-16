advancement revoke @s only dqc.cards:deck_right

execute if score @s dqc.cards.use_rise matches 1.. run function dqc.cards:deck/right_rise
scoreboard players set @s dqc.cards.use_rise 0

# scoreboard players add @s dqc.cards.use_held 1
# execute if score @s dqc.cards.use_held matches 20.. run function dqc.cards:deck/right_redo

scoreboard players set @s dqc.cards.use_fall 2
schedule function dqc.cards:deck/right_check 1t append
