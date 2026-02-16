advancement revoke @s only dqc.cards:place_table
data modify storage dqc.cards:tmp table_color set from entity @s SelectedItem.components."minecraft:custom_model_data".colors[0]
function dqc.cards:table/place_command_block_m with storage dqc.cards:tmp