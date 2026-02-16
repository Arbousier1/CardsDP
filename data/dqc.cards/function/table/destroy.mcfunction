setblock ~ ~ ~ air
schedule function dqc.cards:table/cleanup_all 2t
data modify storage dqc.cards:tmp table_color set from entity @n[type=item_display,tag=dqc.cards.table] item.components."minecraft:custom_model_data".colors[0]
execute unless entity @n[tag=dqc.cards.active_player,gamemode=creative] align xyz run function dqc.cards:table/drop_table_m with storage dqc.cards:tmp