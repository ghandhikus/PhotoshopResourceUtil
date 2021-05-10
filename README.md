# PhotoshopResourceUtil
Simple utility for automating variations of objects with or without the background. Simply change the opacity of Background layer to 0% to skip it. Contains various 

![Photoshop example](https://i.imgur.com/ZnG3dGU.png)

# Usage
Make sure you have JDK or JRE installed.

A. Then you can simply use the tool directly with:
```batch
gradlew run --args="file.psd file2.psd"
```

B. Running the application without arguments will automatically convert all the files from *PSDs* folder to the *Outputs* folder:
```batch
gradlew run
```

C. You can also build a jar to deploy it to the server. Which you can then find in `build\libs\PhotoshopResourceUtil-{VERSION_NAME}-all.jar`
```batch
gradlew shadowJar
```

# Creating PSD file
Make sure you create a Background layer even if you don't use it. If you don't want to export it to it's own file set it's opacity to 0%. Keep in mind it will also ignore that background for everything else.(photoshop isn't very keen on saving empty layers properly.)

# Settings
To change some export settings without changing the code take a look at `.\src\main\java\psdutil\Settings.java`

# Examples
bucket.psd
![bucket.psd.png](https://i.imgur.com/Axhgan0.png)
- Uses Background as a base of the image
- Petunia and Shine Stone - Use groups to merge parts of the image together
- Simply drawn ash and water layers are treated as their own export
- Group names and 0 depth layers are treated as their own exports

Wblock.psd
![Wblock.psd.png](https://i.imgur.com/Jm6s7PG.png)
- Real world example of image with no repeating patterns, with only its own variation.
- Background layer has opacity set to 0% in order to ignore it (it needs to be there because a lot of stuff are hardcoded atm)
