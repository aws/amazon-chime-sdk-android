## Amazon Chime SDK for Android

### Build video calling, audio calling, and screen sharing applications powered by Amazon Chime.

The Amazon Chime SDK makes it easy to add collaborative audio calling,
video calling, and screen share viewing features to Android applications by 
using the same infrastructure services that power millions of Amazon Chime
online meetings.

This Amazon Chime SDK for Android works by connecting to meeting session
resources that you have created in your AWS account. The SDK has everything
you need to build custom calling and collaboration experiences in your
Android application, including methods to: configure meeting sessions, list 
and select audio devices, switch video devices, start and stop screen share 
viewing, receive callbacks when media events occur such as volume changes, 
and control meeting features such as audio mute and video tile bindings.

To get started, see the following resources:

* [Amazon Chime](https://aws.amazon.com/chime)
* [Amazon Chime Developer Guide](https://docs.aws.amazon.com/chime/latest/dg/what-is-chime.html)
* [Amazon Chime SDK API Reference](http://docs.aws.amazon.com/chime/latest/APIReference/Welcome.html)
* TODO - Link to API documentation

And review the following guides:

* [Getting Started](https://aws.github.io/amazon-chime-sdk-android/modules/gettingstarted.html)
* TODO - Verify that above link works once in aws org

### Setup

For the purpose of setup, your project's root folder will be referred to as `root`

#### 1. Download binaries

Note: We are working on making the binaries available on Maven

Download the following binaries and copy them to `root/app/libs`
* TODO - Link to URL for `amazon-chime-sdk.aar`
* TODO - Link to URL for `amazon-chime-sdk-media.aar`

#### 2. Update gradle files

Update `build.gradle` in `root` by adding the following under `repositories` in `allprojects`:

```
allprojects {
   repositories {
      jcenter()
      flatDir {
        dirs 'libs'
      }
   }
}
```

Update `build.gradle` in `root/app` and add the following under `dependencies`:

```
implementation(name: 'amazon-chime-sdk', ext: 'aar')
implementation(name: 'amazon-chime-sdk-media', ext: 'aar')
```

Update `build.gradle` in `root/app` under `compileOptions`:

```
compileOptions {
    sourceCompatibility JavaVersion.VERSION_1_8
    targetCompatibility JavaVersion.VERSION_1_8
}
```

### Running the demo app

To run the demo application:

1. Deploy the serverless demo from [amazon-chime-sdk-js](https://github.com/aws/amazon-chime-sdk-js)

2. Download the following binary and copy it to `amazon-chime-sdk-android/amazon-chime-sdk/libs`
    * TODO - Link to URL for `amazon-chime-sdk-media.aar`

3. Update `test_url` in `strings.xml` at the path `amazon-chime-sdk-android/app/src/main/res/values` with the URL of the serverless demo deployed in Step 1

## Reporting a suspected vulnerability

If you discover a potential security issue in this project we ask that you notify AWS/Amazon Security via our
[vulnerability reporting page](http://aws.amazon.com/security/vulnerability-reporting/).
Please do **not** create a public GitHub issue.

Copyright 2019-2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
