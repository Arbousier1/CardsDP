advancement revoke @s only dqc.cards:used_joker

data modify storage dqc.cards:tmp hand set from entity @s SelectedItem
data modify storage dqc.cards:tmp joker_color set from entity @s SelectedItem.components."minecraft:custom_model_data".colors[0]
function dqc.cards:spawn_jokers_m with storage dqc.cards:tmp