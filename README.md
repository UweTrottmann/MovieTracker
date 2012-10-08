MovieTracker
========================

This GitHub repository hosts the code for the Android app MovieTracker.

Branch structure
----------------

The repository is made up of two main development branches: master (stable) and dev.

* **master** has the latest stable code, its tags are released as the stable version.
* **dev** includes the latest unstable code, no releases are made of it.

Contributing
------------

Want to contribute? Great! Fork the repository, code, tell me about it!

To setup your environment clone the repository. Then import the three projects SeriesGuide, ActionBarSherlock and ViewPagerIndicator into eclipse. To successfully build, you should create a keys.xml file in the SeriesGuideMovies/res/values folder and add the string values 

    <resources>
        <string name="tvdb_apikey"></string>
        <string name="getglue_consumer_key"></string>
        <string name="getglue_consumer_secret"></string>
        <string name="trakt_apikey"></string>
    </resources>
	
to it.

License
-------

    Copyright 2012 Uwe Trottmann

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.