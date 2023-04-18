# Minimal Demo Application

> Note: This uses 0.18.0 SDK version and 0.18.0 media SDK

## Prerequisites
1. Physical Android device
2. Aws account to deploy serverless demo
3. Android Studio and Android SDK installed

## Steps to run demo application

1. Deploy serverless demo
Deploy the serverless demo from [amazon-chime-sdk-js](https://github.com/aws/amazon-chime-sdk-js/tree/main/demos/serverless), which returns https://xxxxx.xxxxx.xxx.com/Prod/

Provide https://xxxxx.xxxxx.xxx.com/Prod/ for mobile demo app.

Update `demo_url` of `src/main/res/values/strings.xml` to those url

2. Run `minimaldemo` from Android Studio