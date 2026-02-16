execute if items entity @s weapon.mainhand *[minecraft:custom_data~{dqc.cards:{is_card:true}}] run return run function dqc.cards:stack/click/right/sneak/card
execute if items entity @s weapon.mainhand *[minecraft:custom_data~{dqc.cards:{is_deck:true}}] run return run function dqc.cards:stack/click/right/sneak/deck
execute if items entity @s weapon.mainhand minecraft:player_head run return run function dqc.cards:stack/click/right/sneak/head
execute unless items entity @s weapon.mainhand * run return run function dqc.cards:stack/click/right/sneak/empty