
# Social Network Clone

#### Authentication using Google - Sign in

- using the google_sign_in package
- package gives us a listener that determines the current state of the account.
- used `signInSilently` to automatically log in if already signed in once.
- Alternative options to sign in 
	- using email and password
	- phone no
	- github, facebook, MS, yahoo, twitter, apple


##### Code
``` dart
void initState() { 

	super.initState();
	pageController = PageController();

	//Detects when user signed in.
	//below we have defined a listener that indicates whether the user has signed in or not
	googleSignIn.onCurrentUserChanged.listen((account) { 
	  handleSignIn(account);
	},
	onError: (e) {
	  print('Error signing in : $e');
	});

	//Reauthenticate user when app is opened
	googleSignIn.signInSilently(suppressErrors: false)
	  .then((account) {
		handleSignIn(account);
	  }).catchError((err) {
		print('Error signing in silently: $err');
	  });

}

handleSignIn(GoogleSignInAccount account) async {

	if( account != null)
	{
	  await createUserInFirestore();
	  setState(() {
		isAuth = true;
	  });
	  configurePushNotifications();
	} else {
	  setState(() {
		isAuth = false;
	  });
	}
}
```

#### Search Users by display name

``` dart
handleSearch(String query) {
    Future<QuerySnapshot> users = usersRef
      .where('displayName', isGreaterThanOrEqualTo: query)
      .getDocuments();

    setState(() {
      searchResultsFuture = users;
    });

}
```

- How do You Optimize search functionality?
	- Tries?


---
#### Geo locator
- Flutter package
- Needs user location access
- Takes in an accuracy as parameter

``` dart
getUserLocation() async {
    Position position = await Geolocator()
        .getCurrentPosition(desiredAccuracy: LocationAccuracy.high);
    List<Placemark> placemarks = await Geolocator()
        .placemarkFromCoordinates(position.latitude, position.longitude);
    Placemark placemark = placemarks[0];
    // String completeAddress =
    //     '${placemark.subThoroughfare} ${placemark.thoroughfare}, ${placemark.subLocality} ${placemark.locality}, ${placemark.subAdministrativeArea}, ${placemark.administrativeArea} ${placemark.postalCode}, ${placemark.country}';
    // print(completeAddress);
    // String completeAddress ='${placemark.subThoroughfare} $
    // {placemark.thoroughfare}, ${placemark.subLocality} $
    // {placemark.locality}, ${placemark.subAdministrativeArea}, $
    // {placemark.administrativeArea} ${placemark.postalCode}, $
    // {placemark.country}';
    // print(completeAddress);
    String formattedAddress = "${placemark.locality}, ${placemark.country}";
    locationController.text = formattedAddress;

 }
```

#### Like/Unlike

``` dart
int getLikeCount(likes) {
	//if no likes return 0
	if(likes == null)
	return 0;

	int count = 0;
	//if the key is explicitly set to true, add a like
	likes.values.forEach((val) {
	  if(val == true )
		count+=1;
	});
	return count;
}


handleLikePost() {
    bool _isLiked = likes[currentUserId] == true ;

    if(_isLiked) {
      postsRef
        .document(ownerId)
        .collection('userPosts')
        .document(postId)
        .updateData({'likes.$currentUserId' : false});

      removeLikeFromActivityFeed();

      setState(() {
        likeCount -= 1;
        isLiked = false;
        likes[currentUserId] = false;
      });
    } else if (!_isLiked) {
      postsRef
        .document(ownerId)
        .collection('userPosts')
        .document(postId)
        .updateData({'likes.$currentUserId' : true});
      
      addLikeToActivityFeed();

      setState(() {
        likeCount += 1;
        isLiked = true;
        likes[currentUserId] = true;
        showHeart = true;
      });
      Timer(Duration(milliseconds: 500), () { 
        setState(() {
          showHeart = false;
        });
      });
    }
}
```

---

#### Activity Feed Notifications

- Three types:
	1. When a user likes your post
	2. When a user comments on your post
	3. When a user follows you
- Database structure
	- Feed/userId/feedItems/ -->will contain all notifications


##### Like Notification
- Implemented in such a way that, we get notified about the most recent like only, for a given post.


``` dart
addLikeToActivityFeed() {
	//add a notification to the postOwner's activity feed only if comment made by 
	// OTHER user (to avoid getting notifications for our own like)
	bool isNotPostOwner = currentUserId != ownerId;
	if(isNotPostOwner) {
	  activityFeedRef
		.document(ownerId)
		.collection('feedItems')
		.document(postId)
		.setData({
		  'type': 'like',
		  'username': currentUser.username,
		  'userId': currentUser.id,
		  'userProfileImg': currentUser.photoUrl,
		  'postId': postId,
		  'mediaUrl': mediaUrl,
		  'timestamp': timestamp
		});  
	}
}
```

##### Comment Notification

``` dart
addComment() {
	commentsRef
	  .document(postId)
	  .collection('comments')
	  .add({
		'username': currentUser.username,
		'comment': commentController.text,
		'timestamp': timestamp,
		'avatarUrl': currentUser.photoUrl,
		'userId': currentUser.id
	  });


	bool isNotPostOwner = postOwnerId != currentUser.id;
	if(isNotPostOwner) {
		activityFeedRef
		.document(postOwnerId)
		.collection('feedItems')
		.add({
		  'type': 'comment',
		  'commentData': commentController.text,
		  'username': currentUser.username,
		  'userId': currentUser.id,
		  'userProfileImg': currentUser.photoUrl,
		  'postId': postId,
		  'mediaUrl': postMediaUrl,
		  'timestamp': timestamp
		});
	}

	commentController.clear();
}
```

---

#### Timeline - Home Page
- [Watch](https://www.youtube.com/watch?v=0_NGCpQzZF0&list=PL78sHffDjI77iyUND7TrUOXud9NSf0eCr&index=51)

![[Pasted image 20210822112518.png]]

- We need to display the posts of all the users that we follow on our timeline(including their past work and their present work).


- **Display the past work: **
	- We listen for changes in the `followers` collection. Every time we follow a person, we can get his user id from the `followers` collection.
	- Using this id, we fetch all the posts from the `posts` collection	(`posts`  structure --> posts/userId/userPosts/..)


<br>


- **Display future work:**
	- Once we have followed a user, we also listen on the posts collection.
	- If any change has been made to a particular post(eg: likes, comments), we find the set of people following the owner of this particular post and update the post in their respective timelines.


- Now all these is handled through another collection called `timeline`,  where we add the post objects that a user should see on his timeline.
- We listen to the collections using `Firestore Triggers`.

![[Pasted image 20210822113618.png]]