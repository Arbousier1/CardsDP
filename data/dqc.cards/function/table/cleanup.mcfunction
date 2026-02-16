execute align xyz as @e[tag=dqc.cards.stack,type=item_display,dx=0,dy=0,dz=0] at @s run function dqc.cards:stack/drop_cards

execute align xyz run kill @e[tag=dqc.cards.stack,dx=0,dy=0,dz=0]
kill @n[type=item_display,tag=dqc.cards.table]
kill @s