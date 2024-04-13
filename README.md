# Skin Grabber
Grabs the skin of the nearby player, to use run `/grabskin`

## How to use
Run `/grabskin` to grab the skin of the nearest player, the player must be within a radius of 5 blocks.

The name of the player will be displayed in the chat, and the URL which you can click to open.

In addition, check the console after running the command for the full decoded skin data.
This includes the skin URL, cape URL (if one is currently equipped), timestamp, profile id, and profile name.

## Example output
![Screenshot of /grabskin in-game](.github/ingame.png)
```
Grabbing Skin!
-----------------------
Skin: ewogICJ0aW1lc3RhbXAiIDogMTcxMzAwMTY0ODg4OCwKICAicHJvZmlsZUlkIiA6ICI3NTg5YjExYmZiZWY0MjE3OTQ1NjcwMmIwYjgxYTgzYyIsCiAgInByb2ZpbGVOYW1lIiA6ICJOb3dUaGlzSXNDcmF6eSIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS8xYTRhZjcxODQ1NWQ0YWFiNTI4ZTdhNjFmODZmYTI1ZTZhMzY5ZDE3NjhkY2IxM2Y3ZGYzMTlhNzEzZWI4MTBiIgogICAgfQogIH0KfQ==
Decoded Skin: {
  "timestamp" : 1713001648888,
  "profileId" : "7589b11bfbef42179456702b0b81a83c",
  "profileName" : "NowThisIsCrazy",
  "signatureRequired" : true,
  "textures" : {
    "SKIN" : {
      "url" : "http://textures.minecraft.net/texture/1a4af718455d4aab528e7a61f86fa25e6a369d1768dcb13f7df319a713eb810b"
    }
  }
}
URL: http://textures.minecraft.net/texture/1a4af718455d4aab528e7a61f86fa25e6a369d1768dcb13f7df319a713eb810b
-----------------------
```