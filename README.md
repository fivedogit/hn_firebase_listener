#How to start building a database of HN items and users

Over the past few weeks, I've been building a Hacker News Chrome extension called ["Hackbook"](https://chrome.google.com/webstore/detail/hackbook/logdfcelflpgcbfebibbeajmhpofckjh/) which provides a nice interface for following other users and getting alerts when people reply to you or your karma changes. Observe:

![Hackbook Explainer](https://s3.amazonaws.com/cyrus-general/main_explainer.png)

Obviously, to make the extension possible, I had to come up with a system for reading the HN API, storing information and firing alerts based on whatever changes happen. This repository is the skeleton code for that core functionality of listening to the HN API.

##HowTo:

###Gathering resources:

1. Eclipse. It's all written in Java, and (I think) most Java coders use Eclipse. So get that first, if you don't already have it.
2. [Firebase](https://www.firebase.com/docs/android/) and [Jsoup](http://jsoup.org/download) jars, unless you want to wrangle with Maven for managing dependencies (and you're on your own for that, if so).
3. You'll need an [AWS account](http://aws.amazon.com/). 
4. You'll need an [apache-tomcat](http://tomcat.apache.org/download-70.cgi) folder. Just get the binary core zip file and unzip it somewhere.

###Configuring Eclipse:

1. Install the AWS SDK from http://aws.amazon.com/eclipse (help -> install new software)
1b. Inside eclipse, activate the AWS explorer tab and look for the flag icon. Set your region to your preferred region. I suggest N. VA as the HN Firebase API appears to be in Texas and the performance seemed better than N. California (for whatever reason).
2. Install JST for Eclipse. Just search for "JST" from all sources and install the 3 packages.
3. Unless you want to use command-line git, you can use JGit/EGit within Eclipse. Install them from the same "install new software" menu.
4. Import https://github.com/fivedogit/hn_firebase_listener as a new project. 

Side note: If the git stuff is causing problems (EGit has been giving me issues recently), then just create a brand new AWS Java Web Project, then cut and paste the 3 java files from the repo manually into a package called "club.hackbook.hbfbl".

5. "Side note" method takes care of this automatically, but you'll need an "AwsCredentials.properties" file in your /src folder of the form:

secretKey=b3bniuo3bo3b7yu8fbauibyfu8aybs
accessKey=GBRGASEFHASJFEJHASJHF

6. Set up your build path. Right click project -> properties -> Java build path -> libraries. Add the following:

- AWS SDK
- The two jar files from above (Firebase and Jsoup)
- Web app libraries
- J2EE Runtime library
- JRE System library

If Web app libraries or J2EE Runtime library don't appear as options under "add library", tweak the "Project facets" and "Targeted runtimes" (this is where the apache-tomcat directory comes in) stuff until they do. You may need to get more Web development related packages from the Eclipse software installer.  

7. Configure web.xml to include the following within the "web-app" tags:

`<listener>
    <listener-class>club.hackbook.hnfbl.FirebaseListener</listener-class>
</listener>`

The FirebaseListener implements ServletContextListener. This directive tells tomcat how to handle it.

###Configuring DynamoDB:

1. Go into your AWS console (https://console.aws.amazon.com) and click DynamoDB
2. Create an "hn_users" table with a primary hash index called "id" of type string. That's all. 

![Setting up the users primary index](https://s3.amazonaws.com/cyrus-general/users_primary_index.png)

3a. Create an "hn_items" table with a primary hash index called "id" of type NUMBER.
3b. On the next screen create a Global secondary index: "by-time-index" (hash=by (string), range=time (int))

![Setting up the items primary index](https://s3.amazonaws.com/cyrus-general/items_primary_index.png)
![Setting up the items secondary index](https://s3.amazonaws.com/cyrus-general/items_secondary_index.png)

This secondary index will allow you to query all items by a certain user over a period of time.

4. For throughput on these two tables, you should be fine at read=1 and write=5 for each index, adjusting the knobs after you've gotten it running.

Note: I'm set up in the N. Virginia US-EAST-1 region. If you set your tables up elsewhere, you'll need to change the line in FirebaseListener to point to the right region.

###Running:

If everything is set up correctly, you should be good to go. Right click your hn_firebase_listener project in Eclipse -> run on server -> Apache tomcat 7 (local). 

If it's running correctly, your console should show output as the program reads the HN API and commits items and users to the two database tables you set up.

Please email me c@mailcyr at the United States TLD and I'll try to clarify.

---------------

Licensing: MIT License. See LICENSE.md