# PhotoshopResourceUtil
Simple utility for automating variations of objects with or without the background. Simply change the opacity of Background layer to 0% to skip it. Contains various 

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

# Settings
To change some export settings without changing the code take a look at `.\src\main\java\psdutil\Settings.java`
