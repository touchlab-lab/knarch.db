# KNArch.db

## What is?

This library is primarily an implementation of sqlite for iOS and MacOS, (roughly) compatible with Android, impelented with Kotlin/Native, to faciliate Kotlin multiplatform development.

## KNArch?

K(otlin)N(ative)Arch(itecture)

Sounds like **narc**. We're still working on a release name, but we'll see.

## Build

https://build.appcenter.ms/v0.1/apps/61048136-1ab0-4789-9ae4-7ad6e8df7777/branches/master/badge

The current build runs against the kotlin/kotlin-ios project only. C++ needs more refactoring to
build with the CI server, but the tests effectively cover the C++ portions.

## Status

There's a lot that's different about Kotlin on native platforms from Kotlin on the JVM. The original goal was to simply port AOSP logic, similar to Doppl. However, there are 2 major issues with that approach.

1. Threading in K/N is *very* different. In demo-size projects, this isn't necessarily obvious, but when you start thinking about what needs to exist for a production level app, this becomes obvious very quickly.

2. How architecture will evolve for multiplatform from "Android" isn't necesarilly clear. But it *should* evolve. Simply copying artifacts is probably not the best choice.

The core database functionality works. That's the start. On top of that, a core suite of tests from Android's CTS suite has been ported and applied. This is to verify that what the
classes are supposed to do is what they actually do. The current goal is to create something that can be used in real, production apps by leveraging both AOSP-based logic and tests, to
jumpstart the library.

With that in place, we will work on some next-stage goals simultaneously.

1. Use the library in production application development to get feedback and refine the test suite and ensure a reliable core exists on which to base multiplatform mobile development.

2. Refine the design. Changing the interface without a good reason isn't necessarily useful, but retaining everything from AOSP doesn't make sense either.

TL;DR Yes, put this in your app today. It works. There's a lot that's going to change, but if you're looking at Kotlin Multiplatform for your iOS implementation, I assume you have a high tollerace for change. Our goal is to have solid *functionality* testing to verify that things work as expected, and starting with the AOSP base gives us that.


## SQLite

KNArch.db is an implementation of sqlite for iOS, structurually very similar to Android's sqlite stack. The source is actually ported directly from AOSP to create a homogenous implementation, including relevant verification testing from the Android CTS suite.

On top of the iOS implementation, a common multiplatform definition is available to facilitate shared database code.

## Status

The code is functional but undergoing testing and revision. Please try it out and submit issues.

## Differences

Due to the KN differences, primariy threading but others as well, the internals of the implementation have had fairly significant changes from the original code.

### Threads

Threading is by far the most significant difference. KN is designed in such a way that you can't really share data between threads unless it is completely frozen. Android's SQLite stack has an inherently multithreaded design, which includes connection pools and explicit synchronization. Out of the box, this would be impossible, and still problematic with some custom structures.

For the initial release, **all database access is single threaded**. You can share the sqlite instance between threads, but when called from different threads, actual database calls are serialized. This was actually the model Android had early on, and in many cases you won't notice the difference, but in highly multithreaded environments, or in a "many read" scenario, you could see degraded performance when compared to Android.

As this implementation stabilizes, pooling may be added back, but the way multithreading happens is different and getting around KN's rules is risky, so we'll take this one step at a time.

There are some things *you* should be aware of.

### Transactions

To simplify the implementation, transactions block. That means if you neglect to call "endTransaction", you'll deadlock other threads. If you neglect to call "endTransaction" in any db app you'll probably have issues, but you'll definitely have them here. This is a deliberate limitation and will likely be removed as the framework matures.

You cannot contribute to a transaction from multiple threads. I'm pretty sure you couldn't do that in Android anyway, but just fyi.

### Transaction listeners

You can add a transaction listener, but because the data in SQLiteConnection lives across threads, we 'freeze' them. It is *very* likely that this isn't the functionality you want. We will either need to figure out thread local with pools, or a smarter way to keep that data across threads.
