execute store result entity @s data.health int 0.99 run data get entity @s data.health
execute unless data entity @s {data:{health:0}} run schedule function dqc.cards:table/restore 5t append
execute if data entity @s {data:{health:0}} run function dqc.cards:table/destroy