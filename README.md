# BackChannel
A publishing platform which pairs lock screen background images with related content in the mobile app. A publisher such as a magazine, a photography business, family and friends or anyone really can push new content to subscribers in the form of a silent lock screen wallpaper notification update and more elaborate content to be seen in the app.

**Check out a video demo by clicking on the image below:**

[![YouTube Demo](https://github.com/iojupiter/BackChannel/blob/main/Screenshot%202023-04-03%20at%2017.57.55.png?raw=true)](https://youtu.be/cPqmt4AYxDw)


# Architecture
Elixir provides a high concurrency, distributed and fault tolerant backend to handle simultaneous publish-subscribe communication.
Published multimedia is structured in MongoDB and a call to notify subscribers via Google Firebase notification platform silently notifies subscribers.
Subscriber devices silently fetch and update lock screen image and content in the app.
Mobile app is build with Google Android.

# Technologies used
1. Elixir
2. Android
3. MongoDB
4. HTML, CSS, Javascript
5. Google firebase messenging platform
