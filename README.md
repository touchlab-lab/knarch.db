# KNArch.db

KNArch (Kotlin/Native Architecture) is a set of libraries built to facilitate shared mobile architecture. While Kotlin/Native is intended to target many platforms, most of the library is specifically created to support Android and iOS development.

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

