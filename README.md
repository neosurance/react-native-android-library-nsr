# React Native Android Library
## react-native-android-library-nsr

This project serves nsr to create custom React Native native modules that can later be installed through npm or yarn and easily be used in production.

## Getting started Android

### Installing it as a library in your main project
There are many ways to do this, here's the way I do it:

1. Do `npm install --save git+https://github.com/neosurance/react-native-android-library-nsr.git` in your main project, or `yarn add git+https://github.com/neosurance/react-native-android-library-nsr.git` 

2. Link the library:
    * Add the following to `android/settings.gradle`:
        ```
        include ':react-native-android-library-nsr'
        project(':react-native-android-library-nsr').projectDir = new File(settingsDir, '../node_modules/react-native-android-library-nsr/android')
        ```

    * Add the following to `android/app/build.gradle`:
        ```xml
        ...

        dependencies {
            ...
		    implementation 'androidx.appcompat:appcompat:1.2.0'

            implementation "com.android.support:support-annotations:27.+"	
            implementation 'com.android.support:appcompat-v7:28.0.0'
            	
            implementation "com.facebook.react:react-native:+"  // From node_modules
            	
            implementation 'com.android.support:support-v4:28.0.0'
            implementation 'com.google.android.gms:play-services-location:17.0.0'
            implementation 'com.android.support.constraint:constraint-layout:1.1.2'
            	
            implementation project(':react-native-android-library-nsr')
        }
        ```
    * Add the following to `android/app/src/main/java/**/MainApplication.java`:
        ```java
        package com.motivation;

        import eu.neosurance.sdk.Package;  // add this for react-native-android-library-nsr

        public class MainApplication extends Application implements ReactApplication {

            @Override
            protected List<ReactPackage> getPackages() {
                return Arrays.<ReactPackage>asList(
                    new MainReactPackage(),
                    new Package()     // add this for react-native-android-library-nsr
                );
            }
        }
        ```
4. Open **build.gradle** under **android/app/**
   - find the android { } block
   ```
   minSdkVersion 19
   ```       
        
5. Simply `import/require` it by the name defined in your library's `package.json`:

    ```javascript
    import Neosurance from 'react-native-android-library-nsr'
    Neosurance.setup(JSON.stringify(settings));
    ```
6. You can test and develop your library by importing the `node_modules` library into **Android Studio** if you don't want to install it from _git_ all the time.


### Usage
```javascript
import Neosurance from 'react-native-android-library-nsr';

...

var settings = {
        base_url:"https://...",
        code:"code",
        secret_key:"secret_key",
        dev_mode: true,
        disable_log:false
};

...

if(Platform.OS === 'android') {

    var _self = this;

    Neosurance.setup(JSON.stringify(settings));
    
}

```
