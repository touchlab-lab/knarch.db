# KNArch.db

## What is?

This library is primarily an implementation of sqlite for iOS and MacOS, (roughly) compatible with Android, impelented with Kotlin/Native, to faciliate Kotlin multiplatform development.

## KNArch?

K(otlin)N(ative)Arch(itecture)

Sounds like **narc**. We're still working on a release name, but we'll see.

## Build

![alt text](https://build.appcenter.ms/v0.1/apps/61048136-1ab0-4789-9ae4-7ad6e8df7777/branches/master/badge "Build Badge")

The CI build is run on [MS App Center](https://appcenter.ms/). Currently it runs the Kotlin/Native iOS tests
only, but that's the bulk of the framework. C++ builds are still done locally until we start fixing to
specific K/N releases and sort out include files.

[Looking for help to sort out multiplatform testing](https://github.com/touchlab/knarch.db/issues/38)

## Status

There's a lot that's different about Kotlin on native platforms from Kotlin on the JVM. The original goal was to simply port AOSP logic, similar to Doppl. However, there are 2 major issues with that approach.

1. Threading in K/N is *very* different. In demo-size projects, this isn't always obvious, but when you start thinking about what needs to exist for a production level app, this becomes a big issue.

2. How architecture will evolve for multiplatform from "Android" isn't necessarily clear. But it *should* evolve. Simply copying artifacts is probably not the best choice.

The core database functionality works. On top of that, a core suite of tests from Android's CTS suite has been ported and applied. This is to verify that what the
classes are supposed to do is what they actually do. The current goal is to create something that can be used in real, production apps by leveraging both AOSP-based logic and tests, to
jumpstart the library.

With that in place, we will work on some next-stage goals simultaneously.

1. Use the library in production application development to get feedback and refine the test suite and ensure a reliable core exists on which to base multiplatform mobile development.

2. Refine the design. Changing the interface without a good reason isn't necessarily useful, but retaining everything from AOSP doesn't make sense either.

TL;DR Yes, put this in your app today. It works. There's a lot that's going to change, but if you're looking at Kotlin Multiplatform for your iOS implementation, I assume you have a high tolerance for change. Our goal is to have solid *functionality* testing to verify that things work as expected, and starting with the AOSP base gives us that.

## Samples

There are 2 samples in the repo currently. Both implement a very basic "notepad" app. You can type in a title and a note,
then add it. The list of notes appears below.

### sample-notepad

Simple sample that uses the database code directly with SQL.

### sample-notepad-sqldelight

Uses [SQLDelight multiplatform](https://github.com/square/sqldelight/blob/master/ALPHA.md) to interact with the database.

### Multiplatform

The MP portion of the framework is very much in flux. After getting iOS to function, the common code was minimally
implemented to support testing in apps and providing a driver for SQLDelight. The common code is going to get a
rethink, mostly around initialization, in the near future.

## Differences

Due to the KN differences, primarily threading but others as well, the internals of the implementation have had fairly significant changes from the original code.

### Threads

Threading is by far the most significant difference. KN is designed in such a way that you can't really share data between threads unless it is completely frozen. Android's SQLite stack has an inherently multithreaded design, which includes connection pools and explicit synchronization. Out of the box, this would be impossible with K/N.

For the initial release, **all database access is single threaded**. You can share the sqlite instance between threads, but when called from different threads, actual database calls are serialized.

FYI, this was Android's sqlite model until about 2.2/2.3. See my [largely outdated answer on SO](https://stackoverflow.com/a/3689883/227313). Modern Android Sqlite allows concurrent reads, but writes are
serialized, and only if you enable WAL. If you're not familiar with what I'm talking about, you almost certainly don't need to worry about serialized DB access and our implementation is plenty fine.

As this implementation stabilizes, pooling may be added back. We're going to push that decision off and let the framework and K/N itself mature a bit.

### Transactions

To simplify the implementation, transactions block. That means if you neglect to call "endTransaction", you'll deadlock other threads that try to start transactions. If you neglect to call "endTransaction" in any db app you'll probably have issues, but you'll definitely have them here. This is a deliberate limitation, and will be reviewed when we revisit connection pools and WAL support.

This isn't a huge deal, but you *definitely* want to make sure you're not reading from the main thread if you're doing
big transactions.

### Transaction listeners

Transaction listeners are currently frozen, but we're going to make them thread local. Again, K/N and threads
are not the same, and there are some rules to follow. You can't interact with a transaction from another thread
currently, so making the listener thread local shouldn't be an issue. If you're using transaction listeners,
it may be something to consider.

### Attached databases

This functionality was removed during porting because of threading issues and how relatively uncommon this is. Will be [added back](https://github.com/touchlab/knarch.db/issues/41).

### Custom Functions

Not available. This is a relatively complex feature that will have C++ and K/N repercussions (threading, etc). Something to look into down the road if desired.

## Usage

AFAIK, K/N doesn't currently have a public dependency mechanism, so while waiting on that, using KNArch is fairly manual. We'll write up a post on this later. Basically, you add the multiplatform dependency as usual with Gradle, and on the iOS side, copy the C++ (bc) and Kotlin (klib) artifacts and wire them inside your Konan config. For now, see the sample app.

## Design

Sqlite support in AOSP is implemented by Java classes that talk to C++ through JNI. We're using the same basic architecture. The C++ code from AOSP has been ported to use equivalent K/N types. See the cpp folder for that code.

In kotlin/kotlin-ios you'll find the public interface code. The api structure is roughly similar to the AOSP code, but
there are differences and omissions because various things don't apply and/or aren't supported.

There will be updates to the design docs in the near future. Follow the [Touchlab blog](https://touchlab.co/blog/), [kpgalligan on medium](https://medium.com/@kpgalligan), or the [Touchlab Twitter](https://twitter.com/touchlabhq) for info.
