buildScript(hasSupportLibraries, hasRetrofit, hasGson, hasEvent, hasScribe, hasPicasso, resourcePackageName, hasDragToRefresh, hasGooglePlay, hasMapGrouping, landingActivity, hasAcra, parse) ::= <<
apply plugin: 'com.android.application'
apply plugin: 'android-apt'
def AAVersion = '3.0.1'

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath 'com.neenbedankt.gradle.plugins:android-apt:1.2+'
    }
}

android {
      lintOptions {
          abortOnError false
      }

    compileSdkVersion 21
    buildToolsVersion "21.1.1"

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_7
        targetCompatibility JavaVersion.VERSION_1_7
    }

    defaultConfig {
        minSdkVersion 15
        targetSdkVersion 19
        versionCode 1
        versionName "1.0"
        applicationId "$resourcePackageName$"
        testInstrumentationRunner "android.support.test.runner.AndroidJUnitRunner"
    }

    packagingOptions {
        exclude 'LICENSE.txt'
    }

     signingConfigs {
        release {
           // When executing release tasks, remember to set the passwords. Don't commit to version control though!
            //storeFile file("KEYSTORE FILE")
            storePassword ""
            keyAlias ""
            keyPassword ""
        }
     }

     buildTypes {

        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
            debuggable false
            signingConfig signingConfigs.release
        }

     }

    compileOptions {
         sourceCompatibility JavaVersion.VERSION_1_7
         targetCompatibility JavaVersion.VERSION_1_7
    }

}

apt {
    arguments {
        androidManifestFile variant.outputs[0].processResources.manifestFile
        resourcePackageName android.defaultConfig.applicationId
    }
}

dependencies {
  apt "org.androidannotations:androidannotations:\$AAVersion"
    compile "org.androidannotations:androidannotations-api:\$AAVersion"
    compile 'com.android.support:appcompat-v7:19.+'
    compile fileTree(dir: 'libs', include: ['*.jar'])

    //test libraries
    androidTestCompile 'com.android.support.test.espresso:espresso-core:2.0'
    androidTestCompile ('com.android.support.test.espresso:espresso-contrib:2.0') {
    	exclude group: 'com.android.support', module: 'appcompat'
        exclude group: 'com.android.support', module: 'support-v4'
    }
    androidTestCompile 'com.android.support.test.espresso:espresso-idling-resource:2.0'


 $if(hasSupportLibraries)$compile "com.android.support:support-v4:21.0.3"$endif$
 $if(hasRetrofit)$compile 'com.squareup.retrofit:retrofit:1.5.0'$endif$
 $if(hasGson)$compile group: 'com.google.code.gson', name: 'gson', version: '2.2.4'$endif$
 $if(hasEvent)$compile 'de.greenrobot:eventbus:2.2.0'$endif$
 $if(hasScribe)$compile 'org.scribe:scribe:1.3.6'$endif$
 $if(hasPicasso)$compile 'com.squareup.picasso:picasso:2.3.1'$endif$
 $if(hasDragToRefresh)$compile 'com.github.chrisbanes.actionbarpulltorefresh:library:0.9.3'$endif$
 $if(hasGooglePlay)$compile 'com.google.android.gms:play-services:4.4.+'$endif$
 $if(hasMapGrouping)$compile 'com.google.maps.android:android-maps-utils:0.3+'$endif$
 $if(hasAcra)$compile 'ch.acra:acra:4.5.0'$endif$
 $if(parse)$compile 'com.parse.bolts:bolts-android:1.2.0'$endif$
}

$if(hasScribe)$
repositories {
        maven{
            url "https://raw.github.com/fernandezpablo85/scribe-java/mvn-repo/"
        }
}$endif$
>>
