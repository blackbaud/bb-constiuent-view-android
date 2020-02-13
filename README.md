# bb-constituent-view-android

This repository contains the code for an app that demonstrates how to use the Android
Assistant with Blackbaud SKY. This is exploratory and I encountered some limitations with
what is currently possible.

This is a native Androud application written in Java and tested on API 28.

Author: [Steven Draugel](https://github.com/sdraugel)

## What it has/does

1. An Android Assistant intent to look up a constituent by name - this displays a view with the profile picture, display name, address and phone number.
1. Ability to look up constituents from within the app.
1. Implements Blackbaud OAuth to login, logout, and refresh the access token when expired.
1. Includes some [SKY UX](https://developer.blackbaud.com/skyux/) styling so the app look matches the site users are familiar with, and follows Blackbaud style guidelines.

[![](./Screenshots/MainActivity500h.png)](./Screenshots/MainActivity.png)
[![](./Screenshots/InAppConstitView500h.png)](./Screenshots/InAppConstitView.png)
[![](./Screenshots/SliceView500h.png)](./Screenshots/SliceView.png)

### Included unused code

I experimented with other available Android functionality and found that the implementation
of these pieces either did not work well or were still in developer preview.

#### ConstiuentSliceProvider - Android deeplink intent

I created a slice view that will integrate with the Google app, when no longer in developer preview.
This will allow a developer to display content directly in the Google app without the need for internet connectivity.

## Limitations found

These limitations exist as of Android 10 (API Level 29) and may change in the future.

### Built in intent actions.intent.OPEN_APP_FEATURE

Q: Can I open a view from an installed app using the generic built in actions.intent.OPEN_APP_FEATURE intent

A: No. There is currently a bug preventing this. ([Source](https://issuetracker.google.com/issues/135714718)). 
As a workaround, I created a node.js service and a custom intent that talks directly to SKY API. ([skyapi-android-voice-skill-hello-world](https://github.com/blackbaud/skyapi-android-voice-skill-hello-world))

### SKY API - server required

Because SKY API uses developer secrets for API calls, an intermediate server is
needed to conceal these secrets. That means that SKY API calls should not be made directly
from a mobile application like they are in this demo. Instead, the app should make
calls to your server and your server should make the calls to SKY API using the secrets.

## Additions wanted (contributions welcome)

1. Cover all reasonable SKY UX styling
1. Turn SKY UX styling into a Framework so other Android apps can reuse the code
1. Turn SKY API into a Framework so other Android apps can reuse the code - ideally would autogenerate this from the SKY API swagger
1. Unit tests
1. Localization
1. Accessibility
1. Proper UI for "find a constituent"

### Further exploration/nice-to-have features

1. Integration with the Google app
1. Dark mode

## Getting started

Prerequisites:
* Android Studio - Recommended IDE
* Android SDK, at least API 28
* [Android App Actions Test Tool](https://developers.google.com/assistant/app/test-tool)
* [Android Slice Viewer](https://developer.android.com/guide/slices/getting-started)
* A SKY Developer account with environment access is needed to access data in SKY API

1. Set up your SKY application
    1. Go to [SKY Developer](https://developer.blackbaud.com/apps/) and create an application
    1. Note the application ID and secret
    1. Add a redirect URI `https://host.nxt.blackbaud.com/app-redirect/redirect-androiddemo/android-deeplink`
    1. In file [strings.xml](./app/src/main/res/values/strings.xml), set the following properties:
    ```xml 
    <string name="subscriptionKey"></string>
    <string name="clientId"></string>
    <string name="redirectUri"></string>
    <string name="clientSecret"></string>
    ```
        * Note that the clientSecret is intended to be kept secret and this value should not be 
        included in a production app
    1. Add the application to an environment
        1. Go to the home page of environment where you are an environment admin
        1. Go to Control Panel > Applications
        1. Click Add Application
        1. Enter your Application ID and click Save

### Debugging
To debug the app - Android Studio

1. Select the device you'd like to debug, set breakpoints in any file under the [java directory](./app/src/main/java/com/blackbaud/constitview), then click debug
    * Note, debugging will degrade performance 

## Usage

This is licensed under the MIT License. Please feel free to use this app as a starting
point for your own app that is available to Blackbaud clients. Keep in mind that there
are separate limitations on using Blackbaud's logo, name, and other trademarks as
part of your App Store metadata as per Apple's and Blackbaud's terms of service.

This repository contains a proof of concept and, while it can be used as the basic for your own app,
you should review it for security best practices prior to publication. For example,
for ease of debugging, secrets and stack traces are printed to the console, which
should never be done in a production application.

### Required changes

Before publishing, you'll want to change the bundle ID, group name, etc. from `*com.blackbaud*` to your
own domain.

There are some TODOs included that should be implemented prior to releasing the app. For
example, you should store the user's access token in secure storage rather than as an app
property.

#### Secrets

For simplicity, this app communicates directly with SKY API using the SKY application secret
and subscription key. These values should never be exposed in a front end application. Instead,
you should have a secure server that makes SKY API requests, and the app should communicate
with that server.

#### OAuth

SKY OAuth requires that you use an https redirect so you'll need to
have a simple website or server redirect endpoint for your OAuth redirect url that then
redirects into your app.
If you don't change the custom URL scheme for this app from the current deeplink, the website
used here (https://host.nxt.blackbaud.com/app-redirect/redirect-androiddemo/android-deeplink) will work while
you are experimenting, but you'll want to change this prior to publishing your own app.

Here is an example of a SKY UX Angular page that can be used to redirect back to an app:

```ts
import {
  Component, OnInit
} from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { SkyAppWindowRef } from '@skyux/core';

@Component({
  selector: 'skyapi-oauth-redirect',
  template: `<main>
  <h1>
    You've been redirected here by SKY API
  </h1>
  <p *ngIf="errorMessage">
    Error: {{ errorMessage }}
  </p>
</main>`
})
export class SkyApiOauthRedirectComponent implements OnInit {

  public errorMessage: string;

  constructor(private route: ActivatedRoute, private windowRef: SkyAppWindowRef) {
  }

  public ngOnInit() {

    this.route.queryParams.subscribe(params => {
      let code = params['code'];
      // let state = params['state']; // TODO pass `state` to the request and verify here
      this.errorMessage = params['error'];

      if (!this.errorMessage) {
        let url = `https://host.nxt.blackbaud.com/app-redirect/redirect-androiddemo/android-deeplink?code=${code}`;
        this.windowRef.nativeWindow.location.href = url;
      }
    });
  }
}
```
